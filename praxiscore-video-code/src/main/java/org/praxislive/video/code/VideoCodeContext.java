/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2025 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 *
 */
package org.praxislive.video.code;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import javax.imageio.ImageIO;
import org.praxislive.code.CodeComponent;
import org.praxislive.code.CodeContext;
import org.praxislive.code.PortDescriptor;
import org.praxislive.code.userapi.Async;
import org.praxislive.core.ExecutionContext;
import org.praxislive.core.services.LogLevel;
import org.praxislive.core.types.PBytes;
import org.praxislive.core.types.PError;
import static org.praxislive.video.code.VideoCodeDelegate.MIME_PNG;
import org.praxislive.video.code.userapi.PGraphics;
import org.praxislive.video.code.userapi.PImage;
import org.praxislive.video.render.Surface;
import org.praxislive.video.render.ops.Blit;
import org.praxislive.video.render.ops.ScaledBlit;
import org.praxislive.video.render.utils.BufferedImageSurface;

/**
 *
 *
 */
public class VideoCodeContext extends CodeContext<VideoCodeDelegate> {

    private final static UnaryOperator<Boolean> DEFAULT_RENDER_QUERY = b -> b;

    private final VideoOutputPort.Descriptor output;
    private final VideoInputPort.Descriptor[] inputs;
    private final Map<String, OffScreenGraphicsInfo> offscreen;
    private final Processor processor;
    private final boolean resetOnSetup;

    private boolean setupRequired;
    private UnaryOperator<Boolean> renderQuery = DEFAULT_RENDER_QUERY;

    public VideoCodeContext(VideoCodeConnector connector) {
        super(connector, connector.hasUpdate());
        setupRequired = true;
        output = connector.extractOutput();
        resetOnSetup = !connector.hasInit();

        List<VideoInputPort.Descriptor> ins = new ArrayList<>();

        inputs = portIDs().map(this::getPortDescriptor)
                .filter(VideoInputPort.Descriptor.class::isInstance)
                .map(VideoInputPort.Descriptor.class::cast)
                .toArray(VideoInputPort.Descriptor[]::new);

        offscreen = connector.extractOffScreenInfo();

        processor = new Processor(inputs.length);
    }

    @Override
    protected void configure(CodeComponent<VideoCodeDelegate> cmp, CodeContext<VideoCodeDelegate> oldCtxt) {
        output.port().getPipe().addSource(processor);
        for (VideoInputPort.Descriptor vidp : inputs) {
            processor.addSource(vidp.port().getPipe());
        }
        configureOffScreen((VideoCodeContext) oldCtxt);
        getDelegate().context = this;
    }

    private void configureOffScreen(VideoCodeContext oldCtxt) {
        Map<String, OffScreenGraphicsInfo> oldOffscreen = oldCtxt == null
                ? Collections.EMPTY_MAP : oldCtxt.offscreen;
        offscreen.forEach((id, osgi) -> osgi.attach(this, oldOffscreen.remove(id)));
        oldOffscreen.forEach((id, osgi) -> osgi.release());
    }

    @Override
    protected void onInit() {
        setupRequired = true;
        renderQuery = DEFAULT_RENDER_QUERY;
        try {
            getDelegate().init();
        } catch (Exception e) {
            getLog().log(LogLevel.ERROR, e, "Exception thrown during init()");
        }
//        if (fullStart) {
//            try {
//                getDelegate().starting();
//            } catch (Exception e) {
//                getLog().log(LogLevel.ERROR, e, "Exception thrown during starting()");
//            }
//        }
    }

    @Override
    protected void onStop() {
        offscreen.forEach((id, osgi) -> osgi.release());
    }

    @Override
    protected void tick(ExecutionContext source) {
        try {
            getDelegate().update();
        } catch (Exception e) {
            getLog().log(LogLevel.ERROR, e, "Exception thrown during update()");
        }
    }

    void attachRenderQuery(UnaryOperator<Boolean> renderQuery) {
        this.renderQuery = Objects.requireNonNull(renderQuery);
    }

    void attachRenderQuery(String source, UnaryOperator<Boolean> renderQuery) {
        PortDescriptor pd = getPortDescriptor(source);
        if (pd instanceof VideoInputPort.Descriptor) {
            ((VideoInputPort.Descriptor) pd).attachRenderQuery(renderQuery);
        } else {
            getLog().log(LogLevel.ERROR, "No source found to attach render query : " + source);
        }
    }

    void attachAlphaQuery(String source, UnaryOperator<Boolean> alphaQuery) {
        PortDescriptor pd = getPortDescriptor(source);
        if (pd instanceof VideoInputPort.Descriptor) {
            ((VideoInputPort.Descriptor) pd).attachAlphaQuery(alphaQuery);
        } else {
            getLog().log(LogLevel.ERROR, "No source found to attach alpha query : " + source);
        }
    }

    Async<PBytes> writeImpl(String mimeType, PImage image, int width, int height) {
        if (!MIME_PNG.equals(mimeType)) {
            return Async.failed(PError.of(IllegalArgumentException.class, "Unsupported mime type"));
        }
        if (image instanceof SurfaceBackedImage surfaceImage) {
            try {
                Surface original = surfaceImage.getSurface();
                WriteImageSurface wis = new WriteImageSurface(width, height);
                Surface scratch = original.createSurface(width, height, true);
                ScaledBlit blit = new ScaledBlit();
                scratch.process(blit, original);
                wis.render(scratch);
                scratch.release();
                return getDelegate().async(wis.getImage(), im -> {
                    PBytes.OutputStream os = new PBytes.OutputStream();
                    ImageIO.write(im, "PNG", os);
                    return os.toBytes();
                });
            } catch (Exception ex) {
                return Async.failed(PError.of(ex));
            }
        } else {
            return Async.failed(PError.of(IllegalArgumentException.class, "Unsupported PImage type"));
        }
    }

    private class Processor extends AbstractProcessPipe {

        private SurfacePGraphics pg;
        private SurfacePImage[] images;

        private Processor(int inputs) {
            super(inputs);
            images = new SurfacePImage[inputs];
        }

        @Override
        protected void update(long time) {
            VideoCodeContext.this.update(time);
        }

        @Override
        protected void callSources(Surface output, long time) {
            validateImages(output);
            int count = getSourceCount();
            for (int i = 0; i < count; i++) {
                callSource(getSource(i), images[i].surface, time);
            }
        }

        @Override
        protected boolean isRendering(long time) {
            return renderQuery.apply(super.isRendering(time));
        }

        @Override
        protected void render(Surface output, long time) {
            output.clear();
            if (pg == null || pg.surface != output) {
                pg = new SurfacePGraphics(output);
                setupRequired = true;
            }
            VideoCodeDelegate del = getDelegate();
            del.setupGraphics(pg, output.getWidth(), output.getHeight());
            pg.beginDraw();
            validateOffscreen(output);
            if (setupRequired) {
                invokeSetup(del);
                setupRequired = false;
            }
            invokeDraw(del);
            pg.endDraw();
            endOffscreen();
            releaseImages();
            flush();
        }

        private void validateImages(Surface output) {
            VideoCodeDelegate del = getDelegate();
            for (int i = 0; i < images.length; i++) {
                SurfacePImage img = images[i];
                Surface s1 = img == null ? null : img.surface;
                Surface s2 = inputs[i].validateSurface(s1, output);
                if (s1 != s2) {
                    if (s1 != null) {
                        s1.release();
                    }
                    img = new SurfacePImage(s2);
                    images[i] = img;
                    setImageField(del, inputs[i].getField(), img);
                }
            }
        }

        private void releaseImages() {
            for (SurfacePImage image : images) {
                image.surface.release();
            }
        }

        private void setImageField(VideoCodeDelegate delegate, Field field, PImage image) {
            try {
                field.set(delegate, image);
            } catch (Exception ex) {
                getLog().log(LogLevel.ERROR, ex);
            }
        }

        private void validateOffscreen(Surface output) {
            offscreen.forEach((id, osgi) -> osgi.validate(output));
        }

        private void endOffscreen() {
            offscreen.forEach((id, osgi) -> osgi.endFrame());
        }

        private void invokeSetup(VideoCodeDelegate delegate) {
            if (resetOnSetup) {
                resetAndInitialize();
            }
            try {
                delegate.setup();
            } catch (Exception ex) {
                getLog().log(LogLevel.ERROR, ex, "Exception thrown from setup()");
            }
        }

        private void invokeDraw(VideoCodeDelegate delegate) {
            try {
                delegate.draw();
            } catch (Exception ex) {
                getLog().log(LogLevel.ERROR, ex, "Exception thrown from draw()");
            }
        }

    }

    private static class SurfacePGraphics extends PGraphics implements SurfaceBackedImage {

        private final Surface surface;

        SurfacePGraphics(Surface surface) {
            super(surface.getWidth(), surface.getHeight());
            this.surface = surface;
        }

        @Override
        public Surface getSurface() {
            return surface;
        }

    }

    private static class SurfacePImage extends PImage implements SurfaceBackedImage {

        private final Surface surface;

        public SurfacePImage(Surface surface) {
            super(surface.getWidth(), surface.getHeight());
            this.surface = surface;
        }

        @Override
        public Surface getSurface() {
            return surface;
        }

    }

    private static class WriteImageSurface extends BufferedImageSurface {

        private final Blit blit;

        private WriteImageSurface(int width, int height) {
            super(width, height, true);
            this.blit = new Blit();
        }

        private void render(Surface source) {
            process(blit, source);
        }

        @Override
        protected BufferedImage getImage() {
            return super.getImage();
        }

    }
}
