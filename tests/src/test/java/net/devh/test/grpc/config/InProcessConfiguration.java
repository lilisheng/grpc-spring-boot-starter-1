/*
 * Copyright (c) 2016-2018 Michael Zhang <yidongnan@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.devh.test.grpc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.devh.springboot.autoconfigure.grpc.client.GlobalClientInterceptorRegistry;
import net.devh.springboot.autoconfigure.grpc.client.GrpcChannelFactory;
import net.devh.springboot.autoconfigure.grpc.client.GrpcChannelsProperties;
import net.devh.springboot.autoconfigure.grpc.client.InProcessChannelFactory;
import net.devh.springboot.autoconfigure.grpc.server.GrpcServerFactory;
import net.devh.springboot.autoconfigure.grpc.server.GrpcServerProperties;
import net.devh.springboot.autoconfigure.grpc.server.GrpcServiceDefinition;
import net.devh.springboot.autoconfigure.grpc.server.GrpcServiceDiscoverer;
import net.devh.springboot.autoconfigure.grpc.server.InProcessGrpcServerFactory;
import net.devh.springboot.autoconfigure.grpc.server.codec.GrpcCodecDefinition;

@Configuration
public class InProcessConfiguration {

    @Bean
    GrpcChannelFactory grpcChannelFactory(final GrpcChannelsProperties properties,
            final GlobalClientInterceptorRegistry globalClientInterceptorRegistry) {
        return new InProcessChannelFactory(properties, globalClientInterceptorRegistry);
    }

    @Bean
    GrpcServerFactory grpcServerFactory(final GrpcServerProperties properties,
            final GrpcServiceDiscoverer discoverer) {
        final InProcessGrpcServerFactory factory = new InProcessGrpcServerFactory("test", properties);
        for (final GrpcCodecDefinition codec : discoverer.findGrpcCodec()) {
            factory.addCodec(codec);
        }
        for (final GrpcServiceDefinition service : discoverer.findGrpcServices()) {
            factory.addService(service);
        }
        return factory;
    }

}
