package ru.top.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.top.events.dto.PhotoGroupView;
import ru.top.events.dto.PhotoView;
import ru.top.events.exception.EventNotFoundException;
import ru.top.events.model.Event;
import ru.top.events.model.Photo;
import ru.top.events.model.User;
import ru.top.events.repository.EventRepository;
import ru.top.events.repository.PhotoRepository;
import ru.top.events.repository.UserRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Альбом воспоминаний: загрузка фото на диск и автогруппировка по дням.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/heic");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru"));

    private final PhotoRepository photoRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Transactional
    public Photo upload(Long eventId, Long uploaderId, MultipartFile file, String caption) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Выбери файл");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Можно загружать только изображения");
        }
        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        String original = file.getOriginalFilename() == null ? "photo" : Paths.get(file.getOriginalFilename())
                .getFileName().toString();
        String storedName = UUID.randomUUID() + extensionOf(original);

        try {
            Path dir = eventDir(eventId);
            Files.createDirectories(dir);
            try (var in = file.getInputStream()) {
                Files.copy(in, dir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить фото", e);
        }

        Photo photo = photoRepository.save(Photo.builder()
                .event(event)
                .uploader(uploader)
                .storedName(storedName)
                .originalName(original)
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .caption(caption == null || caption.isBlank() ? null : caption.trim())
                .takenAt(java.time.LocalDateTime.now())
                .build());
        log.info("В альбом мероприятия id={} загружено фото id={}", eventId, photo.getId());
        return photo;
    }

    @Transactional(readOnly = true)
    public Photo get(Long eventId, Long photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Фото не найдено"));
        if (!photo.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Фото относится к другому мероприятию");
        }
        return photo;
    }

    public Path fileOf(Photo photo) {
        return eventDir(photo.getEvent().getId()).resolve(photo.getStoredName());
    }

    @Transactional
    public void delete(Long eventId, Long photoId) {
        Photo photo = get(eventId, photoId);
        try {
            Files.deleteIfExists(fileOf(photo));
        } catch (IOException e) {
            log.warn("Не удалось удалить файл фото id={}: {}", photoId, e.getMessage());
        }
        photoRepository.delete(photo);
    }

    @Transactional(readOnly = true)
    public long count(Long eventId) {
        return photoRepository.countByEventId(eventId);
    }

    /**
     * Автогруппировка: фото собираются по дню съёмки, свежие дни идут первыми.
     */
    @Transactional(readOnly = true)
    public List<PhotoGroupView> groups(Long eventId) {
        Map<LocalDate, List<PhotoView>> byDay = new LinkedHashMap<>();
        for (Photo photo : photoRepository.findByEventIdOrderByTakenAtDesc(eventId)) {
            byDay.computeIfAbsent(photo.getTakenAt().toLocalDate(), d -> new java.util.ArrayList<>())
                    .add(toView(photo));
        }
        return byDay.entrySet().stream()
                .map(e -> new PhotoGroupView(dayLabel(e.getKey()), e.getKey().format(DAY_FMT), e.getValue()))
                .toList();
    }

    private String dayLabel(LocalDate day) {
        LocalDate today = LocalDate.now();
        if (day.equals(today)) return "Сегодня";
        if (day.equals(today.minusDays(1))) return "Вчера";
        return day.format(DAY_FMT);
    }

    private PhotoView toView(Photo photo) {
        return new PhotoView(
                photo.getId(),
                photo.getOriginalName(),
                photo.getCaption(),
                photo.getUploader().getDisplayName(),
                photo.getTakenAt().format(TIME_FMT),
                humanSize(photo.getSizeBytes()));
    }

    private String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " Б";
        if (bytes < 1024 * 1024) return Math.round(bytes / 1024.0) + " КБ";
        return Math.round(bytes / 1024.0 / 1024.0 * 10) / 10.0 + " МБ";
    }

    private Path eventDir(Long eventId) {
        return Paths.get(uploadDir, "events", String.valueOf(eventId)).toAbsolutePath().normalize();
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot).toLowerCase(Locale.ROOT) : "";
    }
}
