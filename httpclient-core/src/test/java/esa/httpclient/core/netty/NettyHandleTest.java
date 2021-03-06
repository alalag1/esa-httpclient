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

import esa.commons.http.HttpVersion;
import esa.commons.netty.core.Buffers;
import esa.commons.netty.http.Http1HeadersImpl;
import esa.httpclient.core.Context;
import esa.httpclient.core.HttpClient;
import esa.httpclient.core.HttpRequest;
import esa.httpclient.core.HttpResponse;
import esa.httpclient.core.Listener;
import esa.httpclient.core.NoopListener;
import esa.httpclient.core.util.Futures;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class NettyHandleTest {

    private final HttpClient client = HttpClient.ofDefault();

    @SuppressWarnings("unchecked")
    @Test
    void testConstructor() {
        final HttpRequest request = mock(HttpRequest.class);
        final Context ctx = mock(Context.class);
        final Listener listener = mock(Listener.class);
        final CompletableFuture<HttpResponse> response = mock(CompletableFuture.class);

        assertThrows(NullPointerException.class, () -> new NettyHandle(null,
                request, ctx, listener, response));
        assertThrows(NullPointerException.class, () -> new NettyHandle(new DefaultHandle(ByteBufAllocator.DEFAULT),
                null, ctx, listener, response));
        assertThrows(NullPointerException.class, () -> new NettyHandle(new DefaultHandle(ByteBufAllocator.DEFAULT),
                request, null, listener, response));
        assertThrows(NullPointerException.class, () -> new NettyHandle(new DefaultHandle(ByteBufAllocator.DEFAULT),
                request, ctx, null, response));
        assertThrows(NullPointerException.class, () -> new NettyHandle(new DefaultHandle(ByteBufAllocator.DEFAULT),
                request, ctx, listener, null));
        new NettyHandle(new DefaultHandle(ByteBufAllocator.DEFAULT), request, ctx, listener, response);
    }

    @Test
    void testOnOpsNormal() {
        final AtomicInteger count = new AtomicInteger();

        final HttpRequest request = client.get("/abc");
        final Context ctx = new Context();
        final Listener listener = new NoopListener();
        final CompletableFuture<HttpResponse> response = new CompletableFuture<>();

        final HandleImpl handle = new HandleImpl(new NettyResponse(true));
        final NettyHandle nHandle = new NettyHandle(handle, request, ctx, listener, response);
        handle.onStart((v) -> {});
        handle.onData((b) -> {});
        handle.onTrailer((t) -> {});
        handle.onEnd(v -> {});
        handle.onError(v -> count.incrementAndGet());

        nHandle.onMessage(null);
        nHandle.onData(null);
        nHandle.onTrailers(null);
        nHandle.onEnd();
        nHandle.onError(new IOException());

        then(response.isDone()).isTrue();
        then(response.isCompletedExceptionally()).isFalse();
        then(count.get()).isEqualTo(0);
    }

    @Test
    void testOnStartError() {
        final HttpRequest request = client.get("/abc");
        final Context ctx = new Context();
        final Listener listener = new NoopListener();
        final CompletableFuture<HttpResponse> response = new CompletableFuture<>();
        final AtomicInteger error = new AtomicInteger();

        final HandleImpl handle = new HandleImpl(new NettyResponse(true));
        final NettyHandle nHandle = new NettyHandle(handle, request, ctx, listener, response);
        handle.onStart((v) -> {
            throw new IllegalArgumentException();
        });
        handle.onError(v -> error.incrementAndGet());

        nHandle.onMessage(null);

        then(response.isDone()).isTrue();
        then(response.isCompletedExceptionally()).isTrue();
        then(Futures.getCause(response)).isInstanceOf(IllegalArgumentException.class);
        then(error.get()).isEqualTo(1);
    }

    @Test
    void testOnEndError() {
        final HttpRequest request = client.get("/abc");
        final Context ctx = new Context();
        final Listener listener = new NoopListener();
        final CompletableFuture<HttpResponse> response = new CompletableFuture<>();
        final AtomicInteger error = new AtomicInteger();

        final HandleImpl handle = new HandleImpl(new NettyResponse(true));
        final NettyHandle nHandle = new NettyHandle(handle, request, ctx, listener, response);
        handle.onEnd((v) -> {
            throw new IllegalArgumentException();
        });
        handle.onError(v -> error.incrementAndGet());

        nHandle.onEnd();
        nHandle.onError(new IOException());

        then(response.isDone()).isTrue();
        then(response.isCompletedExceptionally()).isTrue();
        then(Futures.getCause(response)).isInstanceOf(IllegalArgumentException.class);
        then(error.get()).isEqualTo(1);
    }

    @Test
    void testOnDataError() {
        final HttpRequest request = client.get("/abc");
        final Context ctx = new Context();
        final Listener listener = new NoopListener();
        final CompletableFuture<HttpResponse> response = new CompletableFuture<>();
        final AtomicInteger error = new AtomicInteger();

        final HandleImpl handle = new HandleImpl(new NettyResponse(true));
        final NettyHandle nHandle = new NettyHandle(handle, request, ctx, listener, response);
        handle.onData((v) -> {
            throw new IllegalArgumentException();
        });
        handle.onError(v -> error.incrementAndGet());

        nHandle.onData(null);
        nHandle.onError(new IOException());

        then(response.isDone()).isTrue();
        then(response.isCompletedExceptionally()).isTrue();
        then(Futures.getCause(response)).isInstanceOf(IllegalArgumentException.class);
        then(error.get()).isEqualTo(1);
    }

    @Test
    void testOnTrailerError() {
        final HttpRequest request = client.get("/abc");
        final Context ctx = new Context();
        final Listener listener = new NoopListener();
        final CompletableFuture<HttpResponse> response = new CompletableFuture<>();
        final AtomicInteger error = new AtomicInteger();

        final HandleImpl handle = new HandleImpl(new NettyResponse(true));
        final NettyHandle nHandle = new NettyHandle(handle, request, ctx, listener, response);
        handle.onTrailer((v) -> {
            throw new IllegalArgumentException();
        });
        handle.onError(v -> error.incrementAndGet());

        nHandle.onTrailers(null);
        nHandle.onError(new IOException());

        then(response.isDone()).isTrue();
        then(response.isCompletedExceptionally()).isTrue();
        then(Futures.getCause(response)).isInstanceOf(IllegalArgumentException.class);
        then(error.get()).isEqualTo(1);
    }

    @Test
    void testOnErrorError() {
        final HttpRequest request = client.get("/abc");
        final Context ctx = new Context();
        final Listener listener = new NoopListener();
        final CompletableFuture<HttpResponse> response = new CompletableFuture<>();

        final HandleImpl handle = new HandleImpl(new NettyResponse(true));
        final NettyHandle nHandle = new NettyHandle(handle, request, ctx, listener, response);
        handle.onError((v) -> {
            throw new IllegalArgumentException();
        });

        nHandle.onTrailers(null);
        nHandle.onError(new IOException());

        then(response.isDone()).isTrue();
        then(response.isCompletedExceptionally()).isTrue();
        then(Futures.getCause(response)).isInstanceOf(IOException.class);
    }

    @Test
    void testNoOpsAfterOnError() {
        final HttpRequest request = client.get("/abc");
        final Context ctx = new Context();
        final Listener listener = new NoopListener();
        final CompletableFuture<HttpResponse> response = new CompletableFuture<>();
        final AtomicInteger count = new AtomicInteger();

        final HandleImpl handle = new HandleImpl(new NettyResponse(true));
        final NettyHandle nHandle = new NettyHandle(handle, request, ctx, listener, response);
        handle.onStart(v -> count.incrementAndGet())
                .onData(v -> count.incrementAndGet())
                .onTrailer(v -> count.incrementAndGet())
                .onEnd(v -> count.incrementAndGet());

        nHandle.onError(new RuntimeException());
        nHandle.onMessage(new HttpMessageImpl(200, HttpVersion.HTTP_1_1, new Http1HeadersImpl()));
        nHandle.onData(Buffers.buffer("Hello".getBytes()));
        nHandle.onTrailers(new Http1HeadersImpl());
        nHandle.onEnd();

        then(count.intValue()).isEqualTo(0);
        then(response.isDone()).isTrue();
        then(response.isCompletedExceptionally()).isTrue();
        then(Futures.getCause(response)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void testNoOpsAfterOnEnd() {
        final HttpRequest request = client.get("/abc");
        final Context ctx = new Context();
        final Listener listener = new NoopListener();
        final CompletableFuture<HttpResponse> response = new CompletableFuture<>();
        final AtomicInteger count = new AtomicInteger();

        final HandleImpl handle = new HandleImpl(new NettyResponse(true));
        final NettyHandle nHandle = new NettyHandle(handle, request, ctx, listener, response);
        handle.onStart(v -> count.incrementAndGet())
                .onData(v -> count.incrementAndGet())
                .onTrailer(v -> count.incrementAndGet())
                .onError(v -> count.incrementAndGet());

        nHandle.onEnd();

        nHandle.onMessage(new HttpMessageImpl(200, HttpVersion.HTTP_1_1, new Http1HeadersImpl()));
        nHandle.onData(Buffers.buffer("Hello".getBytes()));
        nHandle.onTrailers(new Http1HeadersImpl());
        nHandle.onError(new RuntimeException());

        then(count.intValue()).isEqualTo(0);
        then(response.isDone()).isTrue();
        then(response.isCompletedExceptionally()).isFalse();
    }

}
