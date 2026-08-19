package ru.top.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.top.events.model.Photo;
import ru.top.events.service.AlbumService;
import ru.top.events.service.CurrentUserService;
import ru.top.events.service.EventService;

@Controller
@RequestMapping("/events/{eventId}/album")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;
    private final EventService eventService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String page(@PathVariable Long eventId, Model model) {
        model.addAttribute("event", eventService.findById(eventId));
        model.addAttribute("groups", albumService.groups(eventId));
        model.addAttribute("photoCount", albumService.count(eventId));
        model.addAttribute("activeTab", "album");
        return "events/album";
    }

    @PostMapping
    public String upload(@PathVariable Long eventId,
                         @RequestParam("file") MultipartFile file,
                         @RequestParam(required = false) String caption,
                         RedirectAttributes redirectAttributes) {
        try {
            albumService.upload(eventId, currentUserService.currentId(), file, caption);
            redirectAttributes.addFlashAttribute("message", "Фото добавлено 📸");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + eventId + "/album";
    }

    @GetMapping("/photos/{photoId}")
    public ResponseEntity<Resource> file(@PathVariable Long eventId, @PathVariable Long photoId) {
        Photo photo = albumService.get(eventId, photoId);
        Resource resource = new FileSystemResource(albumService.fileOf(photo));
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(resource);
    }

    @PostMapping("/photos/{photoId}/delete")
    public String delete(@PathVariable Long eventId,
                         @PathVariable Long photoId,
                         RedirectAttributes redirectAttributes) {
        try {
            albumService.delete(eventId, photoId);
            redirectAttributes.addFlashAttribute("message", "Фото удалено");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + eventId + "/album";
    }
}
