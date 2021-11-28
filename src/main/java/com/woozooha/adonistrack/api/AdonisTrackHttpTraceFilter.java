package com.woozooha.adonistrack.api;

import com.woozooha.adonistrack.aspect.ProfileAspect;
import com.woozooha.adonistrack.domain.*;
import org.springframework.boot.actuate.trace.http.HttpExchangeTracer;
import org.springframework.boot.actuate.trace.http.HttpTrace;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.web.trace.servlet.HttpTraceFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @see AbstractRequestLoggingFilter
 */
public class AdonisTrackHttpTraceFilter extends HttpTraceFilter {

    /**
     * Create a new {@link HttpTraceFilter} instance.
     *
     * @param repository the trace repository
     * @param tracer     used to trace exchanges
     */
    public AdonisTrackHttpTraceFilter(HttpTraceRepository repository, HttpExchangeTracer tracer) {
        super(new HttpTraceRepositoryWrapper(repository), tracer);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        before();

        super.doFilterInternal(request, response, filterChain);
    }

    protected Invocation before() {
        try {
            return ProfileAspect.before((Event) null);
        } catch (Exception e) {
            return null;
        }
    }

    public static class HttpTraceRepositoryWrapper implements HttpTraceRepository {

        private HttpTraceRepository repository;

        public HttpTraceRepositoryWrapper(HttpTraceRepository repository) {
            this.repository = repository;
        }

        @Override
        public List<HttpTrace> findAll() {
            return repository.findAll();
        }

        @Override
        public void add(HttpTrace trace) {
            repository.add(trace);

            after(trace);
        }

        protected void after(HttpTrace trace) {
            Invocation invocation = Context.getEndpointInvocation();

            if (invocation == null) {
                return;
            }

            HttpTrace.Request request = trace.getRequest();
            HttpTrace.Response response = trace.getResponse();

            if (request == null || response == null) {
                return;
            }

            Instant timestamp = trace.getTimestamp();
            Long start = timestamp.toEpochMilli();
            Long duration = trace.getTimeTaken();

            // Request event

            RequestInfo requestInfo = new RequestInfo();
            requestInfo.setMethod(request.getMethod());
            URI uri = request.getUri();
            if (uri != null) {
                requestInfo.setRequestURI(uri.getPath());
                requestInfo.setQueryString(uri.getQuery());
            }
            requestInfo.setHeaders(headers(request.getHeaders()));
            requestInfo.setStart(start);

            invocation.add(new RequestEvent(requestInfo));

            // Response event

            ResponseInfo responseInfo = new ResponseInfo();
            responseInfo.setStatus(response.getStatus());
            HttpStatus httpStatus = HttpStatus.resolve(response.getStatus());
            if (httpStatus != null) {
                responseInfo.setReasonPhrase(httpStatus.getReasonPhrase());
            }
            responseInfo.setHeaders(headers(response.getHeaders()));
            responseInfo.setStart(start + duration);

            ProfileAspect.after(invocation, new ResponseEvent(responseInfo));
        }

        protected List<Header> headers(Map<String, List<String>> htraceHaders) {
            List<Header> headers = new ArrayList<>();
            htraceHaders.forEach((k, v) -> headers.add(new Header(k, v)));

            return headers;
        }

    }

}
