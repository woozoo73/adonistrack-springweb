package com.woozooha.adonistrack.api;

import com.woozooha.adonistrack.aspect.ProfileAspect;
import com.woozooha.adonistrack.domain.Invocation;
import com.woozooha.adonistrack.writer.History;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdonisTrackInvocationController {

    @GetMapping("/adonis-track/invocations")
    public List<Invocation> histories() {
        History history = ProfileAspect.getConfig().getHistory();

        List<Invocation> invocationList = history.getInvocationList();

        return invocationList;
    }

}
