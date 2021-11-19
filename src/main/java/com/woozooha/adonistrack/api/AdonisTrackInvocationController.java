package com.woozooha.adonistrack.api;

import com.woozooha.adonistrack.aspect.ProfileAspect;
import com.woozooha.adonistrack.domain.Invocation;
import com.woozooha.adonistrack.writer.History;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/adonis-track/invocations")
public class AdonisTrackInvocationController {

    @GetMapping
    public List<Invocation.InvocationSummary> getInvocations() {
        History history = ProfileAspect.getConfig().getHistory();

        List<Invocation> invocations = history.getInvocationList();

        return invocations.stream().map(Invocation.InvocationSummary::new).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public Invocation getInvocation(@PathVariable String id) {
        History history = ProfileAspect.getConfig().getHistory();

        List<Invocation> invocations = history.getInvocationList();

        return invocations.stream().filter(i -> id.equals(i.getId())).findFirst().orElse(null);
    }

}
