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

package net.devh.springboot.autoconfigure.grpc.client;

import java.io.File;
import java.util.function.Function;

import javax.net.ssl.SSLException;

import io.grpc.LoadBalancer;
import io.grpc.NameResolver;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContextBuilder;
import net.devh.springboot.autoconfigure.grpc.client.GrpcChannelProperties.Security;

/**
 * This abstract channel factory contains some shared code for other netty based {@link GrpcChannelFactory}s. This class
 * utilizes connection pooling and thus needs to be {@link #close() closed} after usage.
 *
 * @author Michael (yidongnan@gmail.com)
 * @author Daniel Theuke (daniel.theuke@heuboe.de)
 * @since 5/17/16
 */
public abstract class AbstractNettyChannelFactory extends AbstractChannelFactory<NettyChannelBuilder> {

    private final LoadBalancer.Factory loadBalancerFactory;
    private final NameResolver.Factory nameResolverFactory;

    /**
     * Creates a new AbstractNettyChannelFactory with eager initialized references.
     *
     * @param properties The properties for the channels to create.
     * @param loadBalancerFactory The load balancer factory to use.
     * @param nameResolverFactory The name resolver factory to use.
     * @param globalClientInterceptorRegistry The interceptor registry to use.
     */
    public AbstractNettyChannelFactory(final GrpcChannelsProperties properties,
            final LoadBalancer.Factory loadBalancerFactory,
            final NameResolver.Factory nameResolverFactory,
            final GlobalClientInterceptorRegistry globalClientInterceptorRegistry) {
        super(properties, globalClientInterceptorRegistry);
        this.loadBalancerFactory = loadBalancerFactory;
        this.nameResolverFactory = nameResolverFactory;
    }

    /**
     * Creates a new AbstractNettyChannelFactory with partially lazy initialized references.
     *
     * @param <T> The type of the actual factory class or one of its super classes.
     * @param properties The properties for the channels to create.
     * @param loadBalancerFactory The load balancer factory to use.
     * @param nameResolverFactoryCreator The function that creates the name resolver factory.
     * @param globalClientInterceptorRegistry The interceptor registry to use.
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractNettyChannelFactory> AbstractNettyChannelFactory(final GrpcChannelsProperties properties,
            final LoadBalancer.Factory loadBalancerFactory,
            final Function<T, NameResolver.Factory> nameResolverFactoryCreator,
            final GlobalClientInterceptorRegistry globalClientInterceptorRegistry) {
        super(properties, globalClientInterceptorRegistry);
        this.loadBalancerFactory = loadBalancerFactory;
        this.nameResolverFactory = nameResolverFactoryCreator.apply((T) this);
    }

    @Override
    protected NettyChannelBuilder newChannelBuilder(final String name) {
        return NettyChannelBuilder.forTarget(name)
                .loadBalancerFactory(this.loadBalancerFactory)
                .nameResolverFactory(this.nameResolverFactory);
    }

    @Override
    protected void configureSecurity(final NettyChannelBuilder builder, final String name) {
        final GrpcChannelProperties properties = getPropertiesFor(name);

        final NegotiationType negotiationType = properties.getNegotiationType();
        builder.negotiationType(negotiationType);

        if (negotiationType != NegotiationType.PLAINTEXT) {
            final Security security = properties.getSecurity();

            final String authorityOverwrite = security.getAuthorityOverride();
            if (authorityOverwrite != null && !authorityOverwrite.isEmpty()) {
                builder.overrideAuthority(authorityOverwrite);
            }

            final SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient();

            if (security.isClientAuthEnabled()) {
                final File keyCertChainFile = toCheckedFile("keyCertChain", security.getCertificateChainPath());
                final File privateKeyFile = toCheckedFile("privateKey", security.getPrivateKeyPath());
                sslContextBuilder.keyManager(keyCertChainFile, privateKeyFile);
            }

            final String trustCertCollectionPath = security.getTrustCertCollectionPath();
            if (trustCertCollectionPath != null && !trustCertCollectionPath.isEmpty()) {
                final File trustCertCollectionFile = toCheckedFile("trustCertCollection", trustCertCollectionPath);
                sslContextBuilder.trustManager(trustCertCollectionFile);
            }

            try {
                builder.sslContext(sslContextBuilder.build());
            } catch (final SSLException e) {
                throw new IllegalStateException("Failed to create ssl context for grpc client", e);
            }
        }
    }

}
