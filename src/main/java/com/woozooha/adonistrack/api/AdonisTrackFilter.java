package com.woozooha.adonistrack.api;

import com.woozooha.adonistrack.aspect.ProfileAspect;
import com.woozooha.adonistrack.domain.*;
import com.woozooha.adonistrack.util.PatternMatchUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.filter.AbstractRequestLoggingFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * @see AbstractRequestLoggingFilter
 */
public class AdonisTrackFilter extends OncePerRequestFilter {

    private static final int DEFAULT_MAX_PAYLOAD_LENGTH = 1024 * 1024;

    public static final String[] DEFAULT_IGNORE_URI_PATTERNS = new String[]{
            "/adonis-track/*",
            "/webjars/*",
            "/swagger-ui/*",
            "/v3/api-docs*",
    };

    private static boolean includePayload = true;
    private static int maxPayloadLength = DEFAULT_MAX_PAYLOAD_LENGTH;
    private static String[] ignoreUriPatterns = DEFAULT_IGNORE_URI_PATTERNS;

    public static void setIncludePayload(boolean includePayload) {
        AdonisTrackFilter.includePayload = includePayload;
    }

    protected static boolean isIncludePayload() {
        return AdonisTrackFilter.includePayload;
    }

    public static void setMaxPayloadLength(int maxPayloadLength) {
        Assert.isTrue(maxPayloadLength >= 0, "'maxPayloadLength' should be larger than or equal to 0");
        AdonisTrackFilter.maxPayloadLength = maxPayloadLength;
    }

    protected static int getMaxPayloadLength() {
        return maxPayloadLength;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean isFirstRequest = !isAsyncDispatch(request);
        HttpServletRequest requestToUse = request;
        HttpServletResponse responseToUse = response;

        if (isIncludePayload() && isFirstRequest && !(request instanceof ContentCachingRequestWrapper)) {
            requestToUse = new ContentCachingRequestWrapper(request, getMaxPayloadLength());
        }
        if (isIncludePayload() && isFirstRequest && !(response instanceof ContentCachingResponseWrapper)) {
            responseToUse = new ContentCachingResponseWrapper(response);
        }

        Invocation invocation = null;
        boolean shouldLog = shouldLog(requestToUse, responseToUse);
        if (shouldLog && isFirstRequest) {
            invocation = beforeRequest(requestToUse, responseToUse);
        }

        try {
            filterChain.doFilter(requestToUse, responseToUse);
        } finally {
            if (invocation != null) {
                afterRequest(requestToUse, responseToUse, invocation);
            }
            if (responseToUse instanceof ContentCachingResponseWrapper) {
                flush(responseToUse);
            }
        }
    }

    protected boolean shouldLog(HttpServletRequest request, HttpServletResponse response) {
        return !PatternMatchUtils.simpleMatch(ignoreUriPatterns, request.getRequestURI());
    }

    protected Invocation beforeRequest(HttpServletRequest request, HttpServletResponse response) {
        try {
            RequestInfo requestInfo = new RequestInfo(request);
            if (isIncludePayload()) {
                requestInfo.setPayload(this.getMessagePayload((HttpServletRequest) request));
            }
            Event<RequestInfo> event = new RequestInfoEvent(requestInfo);

            return ProfileAspect.before(event);
        } catch (Exception e) {
            return null;
        }
    }

    protected void afterRequest(HttpServletRequest request, HttpServletResponse response, Invocation invocation) {
        try {
            ResponseInfo responseInfo = new ResponseInfo(response);
            HttpStatus httpStatus = HttpStatus.resolve(response.getStatus());
            if (httpStatus != null) {
                responseInfo.setReasonPhrase(httpStatus.getReasonPhrase());
            }

            if (isIncludePayload()) {
                responseInfo.setPayload(this.getMessagePayload((HttpServletResponse) response));
            }

            // Fill request payload.

            Event<ResponseInfo> event = new ResponseInfoEvent(responseInfo);
            if (isIncludePayload()) {
                List<Event<?>> eventList = invocation.getEventList();
                for (Event ev : eventList) {
                    if (ev.getValue() != null && ev.getValue() instanceof RequestInfo) {
                        RequestInfo requestInfo = (RequestInfo) ev.getValue();
                        if (requestInfo != null && requestInfo.getPayload() == null) {
                            requestInfo.setPayload(this.getMessagePayload((HttpServletRequest) request));
                            break;
                        }
                    }
                }
            }

            ProfileAspect.after(invocation, event);
        } catch (Exception e) {
        }
    }

    protected String getMessagePayload(HttpServletRequest request) {
        ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapper != null) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                int length = Math.min(buf.length, getMaxPayloadLength());

                try {
                    return new String(buf, 0, length, wrapper.getCharacterEncoding());
                } catch (UnsupportedEncodingException var6) {
                    return "[unknown]";
                }
            }
        }

        return null;
    }

    protected String getMessagePayload(HttpServletResponse response) {
        ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper != null) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                int length = Math.min(buf.length, getMaxPayloadLength());

                try {
                    return new String(buf, 0, length, wrapper.getCharacterEncoding());
                } catch (UnsupportedEncodingException var6) {
                    return "[unknown]";
                }
            }
        }

        return null;
    }

    protected void flush(HttpServletResponse response) throws IOException {
        ContentCachingResponseWrapper responseWrapper =
                WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        responseWrapper.copyBodyToResponse();
    }

}
