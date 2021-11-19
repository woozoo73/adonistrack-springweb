package com.woozooha.adonistrack.api;

import com.woozooha.adonistrack.aspect.ProfileAspect;
import com.woozooha.adonistrack.domain.*;
import com.woozooha.adonistrack.util.PatternMatchUtils;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.function.Predicate;

public class AdonisTrackInterceptor implements HandlerInterceptor {

    private static ThreadLocal<Invocation> CONTEXT = new ThreadLocal<Invocation>();

    public static final String[] IGNORE_URI_PATTERNS = new String [] {
            "/adonis-track/*",
            "/webjars/*",
    };

    private Predicate<HttpServletRequest> filter = (r) -> {
        return !PatternMatchUtils.simpleMatch(IGNORE_URI_PATTERNS, r.getRequestURI());
    };

    public Predicate<HttpServletRequest> getFilter() {
        return filter;
    }

    public void setFilter(Predicate<HttpServletRequest> filter) {
        this.filter = filter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!getFilter().test(request)) {
            return true;
        }

        Invocation invocation = CONTEXT.get();
        if (invocation == null) {
            invocation = before(request);
            CONTEXT.set(invocation);
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           @Nullable ModelAndView modelAndView) throws Exception {
        if (!getFilter().test(request)) {
            return;
        }

        Invocation invocation = CONTEXT.get();
        if (invocation != null) {
            after(invocation, request, response);
            CONTEXT.set(null);
        }
    }

    private Invocation before(HttpServletRequest request) {
        Event<RequestInfo> event = new RequestInfoEvent(new RequestInfo(request));

        return ProfileAspect.before(event);
    }

    private void after(Invocation invocation, HttpServletRequest request, HttpServletResponse response) {
        Event<ResponseInfo> event = new ResponseInfoEvent(new ResponseInfo(response));

        ProfileAspect.after(invocation, event);
    }

}
