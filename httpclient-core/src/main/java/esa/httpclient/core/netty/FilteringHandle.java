/*
 * Copyright 2020 OPPO ESA Stack Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package esa.httpclient.core.netty;

import esa.commons.Checks;
import esa.commons.http.HttpHeaders;
import esa.commons.netty.core.Buffer;
import esa.httpclient.core.Context;
import esa.httpclient.core.HttpMessage;
import esa.httpclient.core.HttpRequest;
import esa.httpclient.core.HttpResponse;
import esa.httpclient.core.Listener;
import esa.httpclient.core.filter.FilterContext;
import esa.httpclient.core.filter.ResponseFilter;

import java.util.concurrent.CompletableFuture;

class FilteringHandle extends NettyHandle {

    private final ResponseFilter[] filters;
    private final FilterContext ctx;

    private CompletableFuture<Void> handleChain;

    FilteringHandle(HandleImpl handle,
                    HttpRequest request,
                    Context ctx,
                    Listener listener,
                    CompletableFuture<HttpResponse> response,
                    ResponseFilter[] filters,
                    FilterContext fCtx) {
        super(handle, request, ctx, listener, response);
        Checks.checkNotNull(fCtx, "FilterContext must not be null");
        Checks.checkNotEmptyArg(filters, "ResponseFilters must not be empty");
        this.filters = filters;
        this.ctx = fCtx;
    }

    @Override
    public void onMessage(HttpMessage message) {
        super.onMessage(message);

        try {
            for (ResponseFilter filter : filters) {
                if (handleChain == null) {
                    handleChain = filter.doFilter(request, handle.underlying, ctx);
                } else {
                    handleChain = handleChain.thenCompose(v -> filter.doFilter(request, handle.underlying, ctx));
                }
            }
        } catch (Throwable ex) {
            super.onError(ex);
        }

        // handleChain is null means that some exception has occurred before
        // and we should ignore the message in this case.
        if (handleChain == null) {
            return;
        }
        handleChain = handleChain.exceptionally(th -> {
            super.onError(th);
            return null;
        });
    }

    @Override
    public void onData(Buffer content) {
        // handleChain is null means that some exception has occurred before
        // and we should ignore the content in this case.
        if (handleChain == null) {
            return;
        }

        handleChain = handleChain.whenComplete((v, th) -> {
            if (th != null) {
                super.onError(th);
            } else {
                super.onData(content);
            }
        });
    }

    @Override
    public void onEnd() {
        // handleChain is null means that some exception has occurred before
        // and we should ignore the end operation in this case.
        if (handleChain == null) {
            return;
        }

        handleChain = handleChain.whenComplete((v, th) -> {
            if (th != null) {
                super.onError(th);
            } else {
                super.onEnd();
            }
        });

        // Make sure that the exception occurred in handle#onXxx() should be handled normally.
        handleChain.exceptionally(th -> {
            super.onError(th);
            return null;
        });
    }

    @Override
    public void onTrailers(HttpHeaders trailers) {
        // handleChain is null means that some exception has occurred before
        // and we should ignore the trailers in this case.
        if (handleChain == null) {
            return;
        }

        handleChain = handleChain.whenComplete((v, th) -> {
            if (th != null) {
                super.onError(th);
            } else {
                super.onTrailers(trailers);
            }
        });
    }

}

