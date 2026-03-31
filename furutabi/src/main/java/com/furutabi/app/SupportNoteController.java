package com.furutabi.app;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/app/support-notes")
public class SupportNoteController {

    private final SupportNoteService supportNoteService;

    public SupportNoteController(SupportNoteService supportNoteService) {
        this.supportNoteService = supportNoteService;
    }

    @GetMapping("/users/{targetUserId}")
    public String list(@PathVariable long targetUserId, Authentication authentication, Model model) {
        try {
            model.addAttribute("pageData", supportNoteService.loadList(authentication.getName(), targetUserId));
            return "app/support-note-list";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Support note page not found", ex);
        }
    }

    @GetMapping("/users/{targetUserId}/care-points")
    public String carePoints(@PathVariable long targetUserId, Authentication authentication, Model model) {
        try {
            model.addAttribute("pageData", supportNoteService.loadCarePointsPage(authentication.getName(), targetUserId));
            return "app/support-note-care-points";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Support note page not found", ex);
        }
    }

    @GetMapping("/users/{targetUserId}/new")
    public String createPage(
        @PathVariable long targetUserId,
        @RequestParam(name = "relatedCardId", required = false) Long relatedCardId,
        Authentication authentication,
        Model model
    ) {
        try {
            SupportNoteService.SupportNoteEditorPageData pageData = supportNoteService.loadCreatePage(
                authentication.getName(),
                targetUserId,
                relatedCardId
            );
            model.addAttribute("pageData", pageData);
            model.addAttribute("supportNoteForm", pageData.form());
            return "app/support-note-form";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Support note page not found", ex);
        }
    }

    @PostMapping("/users/{targetUserId}")
    public String create(@PathVariable long targetUserId, Authentication authentication, @ModelAttribute("supportNoteForm") SupportNoteForm supportNoteForm) {
        try {
            long noteId = supportNoteService.createNote(authentication.getName(), targetUserId, supportNoteForm);
            return "redirect:/app/support-notes/" + noteId + "?created";
        } catch (SupportNoteService.SupportNoteConflictException ex) {
            return "redirect:/app/support-notes/users/" + targetUserId + "/new?blocked";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Support note page not found", ex);
        }
    }

    @GetMapping("/{noteId}")
    public String detail(@PathVariable long noteId, Authentication authentication, Model model) {
        try {
            model.addAttribute("pageData", supportNoteService.loadDetail(authentication.getName(), noteId));
            model.addAttribute("supportNoteModerationForm", new SupportNoteModerationForm());
            model.addAttribute("supportNoteReportForm", new SupportNoteReportForm());
            return "app/support-note-detail";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Support note not found", ex);
        }
    }

    @GetMapping("/{noteId}/related-card")
    public String relatedCard(@PathVariable long noteId, Authentication authentication, Model model) {
        try {
            model.addAttribute("pageData", supportNoteService.loadRelatedCardPage(authentication.getName(), noteId));
            return "app/support-note-related-card";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Related card not found", ex);
        }
    }

    @GetMapping("/{noteId}/edit")
    public String editPage(@PathVariable long noteId, Authentication authentication, Model model) {
        try {
            SupportNoteService.SupportNoteEditorPageData pageData = supportNoteService.loadEditPage(authentication.getName(), noteId);
            model.addAttribute("pageData", pageData);
            model.addAttribute("supportNoteForm", pageData.form());
            return "app/support-note-form";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Support note not found", ex);
        }
    }

    @PostMapping("/{noteId}")
    public String update(@PathVariable long noteId, Authentication authentication, @ModelAttribute("supportNoteForm") SupportNoteForm supportNoteForm) {
        try {
            supportNoteService.updateNote(authentication.getName(), noteId, supportNoteForm);
            return "redirect:/app/support-notes/" + noteId + "?saved";
        } catch (SupportNoteService.SupportNoteConflictException ex) {
            return "redirect:/app/support-notes/" + noteId + "/edit?blocked";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Support note not found", ex);
        }
    }

    @PostMapping("/{noteId}/hide")
    public String hide(@PathVariable long noteId, Authentication authentication, @ModelAttribute("supportNoteModerationForm") SupportNoteModerationForm supportNoteModerationForm) {
        try {
            supportNoteService.hideNote(authentication.getName(), noteId, supportNoteModerationForm);
            return "redirect:/app/support-notes/" + noteId + "?hidden";
        } catch (SupportNoteService.SupportNoteConflictException ex) {
            return "redirect:/app/support-notes/" + noteId + "?blocked";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Support note not found", ex);
        }
    }

    @PostMapping("/{noteId}/unhide")
    public String unhide(@PathVariable long noteId, Authentication authentication, @ModelAttribute("supportNoteModerationForm") SupportNoteModerationForm supportNoteModerationForm) {
        try {
            supportNoteService.unhideNote(authentication.getName(), noteId, supportNoteModerationForm);
            return "redirect:/app/support-notes/" + noteId + "?unhidden";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Support note not found", ex);
        }
    }

    @PostMapping("/{noteId}/reports")
    public String report(@PathVariable long noteId, Authentication authentication, @ModelAttribute("supportNoteReportForm") SupportNoteReportForm supportNoteReportForm) {
        try {
            supportNoteService.reportNote(authentication.getName(), noteId, supportNoteReportForm);
            return "redirect:/app/support-notes/" + noteId + "?reported";
        } catch (SupportNoteService.SupportNoteConflictException ex) {
            return "redirect:/app/support-notes/" + noteId + "?blocked";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Support note not found", ex);
        }
    }
}
