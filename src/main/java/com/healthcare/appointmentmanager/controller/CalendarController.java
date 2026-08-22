package com.healthcare.appointmentmanager.controller;

import com.healthcare.appointmentmanager.model.AppUser;
import com.healthcare.appointmentmanager.repository.AppUserRepository;
import com.healthcare.appointmentmanager.service.GoogleCalendarService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class CalendarController {

    private final GoogleCalendarService calendarService;
    private final AppUserRepository userRepository;

    public CalendarController(GoogleCalendarService calendarService, AppUserRepository userRepository) {
        this.calendarService = calendarService;
        this.userRepository = userRepository;
    }

    @GetMapping("/calendar/connect")
    public String connect(HttpSession session) {
        if (!calendarService.isConfigured()) return "redirect:/dashboard?calendarNotConfigured";
        String state = UUID.randomUUID().toString();
        session.setAttribute("googleOAuthState", state);
        return "redirect:" + calendarService.authorizationUrl(state);
    }

    @GetMapping("/calendar/callback")
    public String callback(@RequestParam String code,
                           @RequestParam String state,
                           HttpSession session,
                           Authentication authentication) {
        Object expected = session.getAttribute("googleOAuthState");
        session.removeAttribute("googleOAuthState");
        if (expected == null || !expected.equals(state)) return "redirect:/dashboard?calendarError";
        AppUser user = userRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        try {
            calendarService.exchangeAuthorizationCode(user, code);
            return "redirect:/dashboard?calendarConnected";
        } catch (Exception exception) {
            return "redirect:/dashboard?calendarError";
        }
    }

    @PostMapping("/calendar/disconnect")
    public String disconnect(Authentication authentication) {
        AppUser user = userRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        calendarService.disconnect(user.getId());
        return "redirect:/dashboard?calendarDisconnected";
    }
}
