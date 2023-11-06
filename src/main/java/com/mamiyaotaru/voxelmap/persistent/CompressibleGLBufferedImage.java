package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.CompressionUtils;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.zip.DataFormatException;

public class CompressibleGLBufferedImage {
    private byte[] bytes;
    private int index;
    private final int width;
    private final int height;
    private final Object bufferLock = new Object();
    private boolean isCompressed;
    private static final HashMap<Integer, ByteBuffer> byteBuffers = new HashMap<>(4);
    private static final ByteBuffer defaultSizeBuffer = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder());
    private final boolean compressNotDelete;

    public CompressibleGLBufferedImage(int width, int height, int imageType) {
        this.width = width;
        this.height = height;
        this.bytes = new byte[width * height * 4];
        this.compressNotDelete = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions().outputImages;
    }

    public byte[] getData() {
        if (this.isCompressed) {
            this.decompress();
        }

        return this.bytes;
    }

    public int getIndex() {
        return this.index;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void baleet() {
        int currentIndex = this.index;
        this.index = 0;
        if (currentIndex != 0 && RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.deleteTexture(currentIndex);
        }

    }

    public void write() {
        if (this.isCompressed) {
            this.decompress();
        }

        if (this.index == 0) {
            this.index = GlStateManager._genTexture();
        }

        ByteBuffer buffer = byteBuffers.get(this.width * this.height);
        if (buffer == null) {
            buffer = ByteBuffer.allocateDirect(this.width * this.height * 4).order(ByteOrder.nativeOrder());
            byteBuffers.put(this.width * this.height, buffer);
        }

        buffer.clear();
        synchronized (this.bufferLock) {
            buffer.put(this.bytes);
        }

        buffer.position(0).limit(this.bytes.length);
        RenderSystem.bindTexture(this.index);
        RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_NEAREST);
        RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_NEAREST);
        RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_S, OpenGL.GL12_GL_CLAMP_TO_EDGE);
        RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_T, OpenGL.GL12_GL_CLAMP_TO_EDGE);
        RenderSystem.pixelStore(OpenGL.GL11_GL_UNPACK_ROW_LENGTH, 0);
        RenderSystem.pixelStore(OpenGL.GL11_GL_UNPACK_SKIP_PIXELS, 0);
        RenderSystem.pixelStore(OpenGL.GL11_GL_UNPACK_SKIP_ROWS, 0);
        OpenGL.glTexImage2D(OpenGL.GL11_GL_TEXTURE_2D, 0, OpenGL.GL11_GL_RGBA, this.getWidth(), this.getHeight(), 0, OpenGL.GL11_GL_RGBA, OpenGL.GL12_GL_UNSIGNED_INT_8_8_8_8, buffer);
        OpenGL.glGenerateMipmap(OpenGL.GL11_GL_TEXTURE_2D);
        this.compress();
    }

    public void setRGB(int x, int y, int color) {
        if (this.isCompressed) {
            this.decompress();
        }

        int index = (x + y * this.getWidth()) * 4;
        synchronized (this.bufferLock) {
            int alpha = color >> 24 & 0xFF;
            this.bytes[index] = -1;
            this.bytes[index + 1] = (byte) ((color & 0xFF) * alpha / 255);
            this.bytes[index + 2] = (byte) ((color >> 8 & 0xFF) * alpha / 255);
            this.bytes[index + 3] = (byte) ((color >> 16 & 0xFF) * alpha / 255);
        }
    }

    private synchronized void compress() {
        if (!this.isCompressed) {
            if (this.compressNotDelete) {
                try {
                    this.bytes = CompressionUtils.compress(this.bytes);
                } catch (IOException ignored) {}
            } else {
                this.bytes = null;
            }

            this.isCompressed = true;
        }
    }

    private synchronized void decompress() {
        if (this.isCompressed) {
            if (this.compressNotDelete) {
                try {
                    this.bytes = CompressionUtils.decompress(this.bytes);
                } catch (IOException | DataFormatException ignored) {}
            } else {
                this.bytes = new byte[this.width * this.height * 4];
                this.isCompressed = false;
            }

        }
    }

    static {
        byteBuffers.put(65536, defaultSizeBuffer);
    }
}
