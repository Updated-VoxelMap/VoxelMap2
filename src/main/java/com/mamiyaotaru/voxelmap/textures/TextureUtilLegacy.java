package com.mamiyaotaru.voxelmap.textures;

import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public final class TextureUtilLegacy {
    private static final IntBuffer DATA_BUFFER = createDirectIntBuffer(4194304);

    private TextureUtilLegacy() {}

    public static synchronized ByteBuffer createDirectByteBuffer(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    public static IntBuffer createDirectIntBuffer(int capacity) {
        return createDirectByteBuffer(capacity << 2).asIntBuffer();
    }

    public static BufferedImage readBufferedImage(InputStream imageStream) throws IOException {
        if (imageStream == null) {
            return null;
        } else {
            BufferedImage bufferedimage;
            try {
                bufferedimage = ImageIO.read(imageStream);
            } finally {
                IOUtils.closeQuietly(imageStream);
            }

            return bufferedimage;
        }
    }

    public static void allocateTextureImpl(int glTextureId, int mipmapLevels, int width, int height) {
        bindTexture(glTextureId);
        if (mipmapLevels >= 0) {
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, 33085, mipmapLevels);
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, 33082, 0);
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, 33083, mipmapLevels);
            GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, 34049, 0.0F);
        }

        for (int i = 0; i <= mipmapLevels; ++i) {
            RenderSystem.pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
            RenderSystem.pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
            RenderSystem.pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
            GlStateManager._texImage2D(GlConst.GL_TEXTURE_2D, i, GlConst.GL_RGBA, width >> i, height >> i, 0, OpenGL.GL_BGRA, OpenGL.GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer) null);
        }

    }

    public static void uploadTexture(int glTextureId, int[] zeros, int currentImageWidth, int currentImageHeight) {
        bindTexture(glTextureId);
        uploadTextureSub(0, zeros, currentImageWidth, currentImageHeight, 0, 0, false, false, false);
    }

    private static void copyToBufferPos(int[] imageData, int p_110994_1_, int p_110994_2_) {
        DATA_BUFFER.clear();
        DATA_BUFFER.put(imageData, p_110994_1_, p_110994_2_);
        DATA_BUFFER.position(0).limit(p_110994_2_);
    }

    static void bindTexture(int id) {
        RenderSystem.bindTexture(id);
    }

    public static void uploadTextureMipmap(int[][] textureData, int width, int height, int originX, int originY, boolean blurred, boolean clamped) {
        for (int i = 0; i < textureData.length; ++i) {
            int[] aint = textureData[i];
            uploadTextureSub(i, aint, width >> i, height >> i, originX >> i, originY >> i, blurred, clamped, textureData.length > 1);
        }

    }

    public static void setTextureClamped(boolean clamped) {
        if (clamped) {
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_WRAP_S, GlConst.GL_CLAMP_TO_EDGE);
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_WRAP_T, GlConst.GL_CLAMP_TO_EDGE);
        } else {
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_WRAP_S, 10497);
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_WRAP_T, 10497);
        }

    }

    public static void setTextureBlurMipmap(boolean blurred, boolean mipmapped) {
        if (blurred) {
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MIN_FILTER, mipmapped ? GlConst.GL_LINEAR_MIPMAP_LINEAR : GlConst.GL_LINEAR);
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MAG_FILTER, GlConst.GL_LINEAR);
        } else {
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MIN_FILTER, mipmapped ? 9986 : GlConst.GL_NEAREST);
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MAG_FILTER, GlConst.GL_NEAREST);
        }

    }

    private static void uploadTextureSub(int mipmapLevel, int[] imageData, int width, int height, int originX, int originY, boolean blurred, boolean clamped, boolean mipmapped) {
        int maxRows = 4194304 / width;
        setTextureBlurMipmap(blurred, mipmapped);
        setTextureClamped(clamped);
        RenderSystem.pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, width);
        RenderSystem.pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
        RenderSystem.pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);

        int rowsToCopy;
        for (int pos = 0; pos < width * height; pos += width * rowsToCopy) {
            int rowsCopied = pos / width;
            rowsToCopy = Math.min(maxRows, height - rowsCopied);
            int sizeOfCopy = width * rowsToCopy;
            copyToBufferPos(imageData, pos, sizeOfCopy);
            GlStateManager._texImage2D(GlConst.GL_TEXTURE_2D, mipmapLevel, originX, originY + rowsCopied, width, rowsToCopy, OpenGL.GL_BGRA, OpenGL.GL_UNSIGNED_INT_8_8_8_8_REV, DATA_BUFFER);
        }

    }
}
