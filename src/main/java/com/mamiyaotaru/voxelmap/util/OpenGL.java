package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class OpenGL {
    public static final int
            GL11_GL_DEPTH_BUFFER_BIT     = 0x100,
            GL11_GL_LEQUAL               = 0x203,
            GL11_GL_ALWAYS               = 0x207,
            GL11_GL_SRC_ALPHA            = 0x302,
            GL11_GL_ONE_MINUS_SRC_ALPHA  = 0x303,
            GL11_GL_DST_COLOR            = 0x306,
            GL11_GL_UNPACK_ROW_LENGTH    = 0xCF2,
            GL11_GL_UNPACK_SKIP_ROWS     = 0xCF3,
            GL11_GL_UNPACK_SKIP_PIXELS   = 0xCF4,
            GL11_GL_UNPACK_ALIGNMENT     = 0xCF5,
            GL11_GL_PACK_ALIGNMENT       = 0xD05,
            GL11_GL_TEXTURE_2D           = 0xDE1,
            GL11_GL_TRANSFORM_BIT        = 0x1000,
            GL11_GL_TEXTURE_HEIGHT       = 0x1001,
            GL11_GL_BYTE                 = 0x1400,
            GL11_GL_UNSIGNED_BYTE        = 0x1401,
            GL11_GL_RGBA                 = 0x1908,
            GL11_GL_NEAREST              = 0x2600,
            GL11_GL_LINEAR               = 0x2601,
            GL11_GL_LINEAR_MIPMAP_LINEAR = 0x2703,
            GL11_GL_TEXTURE_MAG_FILTER   = 0x2800,
            GL11_GL_TEXTURE_MIN_FILTER   = 0x2801,
            GL11_GL_TEXTURE_WRAP_S       = 0x2802,
            GL11_GL_TEXTURE_WRAP_T       = 0x2803,
            GL11_GL_COLOR_BUFFER_BIT     = 0x4000,
            GL11_GL_TEXTURE_BINDING_2D   = 0x8069;

    public static final int
            GL12_GL_UNSIGNED_INT_8_8_8_8     = 0x8035,
            GL12_GL_BGRA                     = 0x80E1,
            GL12_GL_CLAMP_TO_EDGE            = 0x812F,
            GL12_GL_UNSIGNED_INT_8_8_8_8_REV = 0x8367;

    public static final int
            GL14_GL_DEPTH_COMPONENT24 = 0x81A6;

    public static final int
            GL30_GL_FRAMEBUFFER_BINDING                       = 0x8CA6,
            GL30_GL_READ_FRAMEBUFFER_BINDING                  = 0x8CAA,
            GL30_GL_FRAMEBUFFER                               = 0x8D40,
            GL30_GL_READ_FRAMEBUFFER                          = 0x8CA8,
            GL30_GL_DRAW_FRAMEBUFFER                          = 0x8CA9,
            GL30_GL_FRAMEBUFFER_COMPLETE                      = 0x8CD5,
            GL30_GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT         = 0x8CD6,
            GL30_GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7,
            GL30_GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER        = 0x8CDB,
            GL30_GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER        = 0x8CDC,
            GL30_GL_COLOR_ATTACHMENT0                         = 0x8CE0,
            GL30_GL_DEPTH_ATTACHMENT                          = 0x8D00,
            GL30_GL_RENDERBUFFER                              = 0x8D41;

    private OpenGL() {}

    public static void glGenerateMipmap(int target) { GL30.glGenerateMipmap(target); }

    public static final class Utils {
        public static final Tessellator TESSELLATOR = Tessellator.getInstance();
        public static final BufferBuilder VERTEX_BUFFER = TESSELLATOR.getBuffer();
        public static final IntBuffer DATA_BUFFER = GlAllocationUtils.allocateByteBuffer(16777216).asIntBuffer();

        public static final TextureManager textureManager = VoxelConstants.getMinecraft().getTextureManager();
        public static int fboId = -1;
        public static int fboTextureId = -1;
        public static int previousFboId = -1;
        public static int previousFboIdRead = -1;
        public static int previousFboIdDraw = -1;

        private Utils() {}

        public static void setupFramebuffer() {
            previousFboId = GlStateManager._getInteger(GL30_GL_FRAMEBUFFER_BINDING);
            fboId = GlStateManager.glGenFramebuffers();
            fboTextureId = GlStateManager._genTexture();

            int width = 512;
            int height = 512;
            ByteBuffer buffer = BufferUtils.createByteBuffer(4 * width * height);

            GlStateManager._glBindFramebuffer(GL30_GL_FRAMEBUFFER, fboId);
            RenderSystem.bindTexture(fboTextureId);
            RenderSystem.texParameter(GL11_GL_TEXTURE_2D, GL11_GL_TEXTURE_WRAP_S, GL12_GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11_GL_TEXTURE_2D, GL11_GL_TEXTURE_WRAP_T, GL12_GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11_GL_TEXTURE_2D, GL11_GL_TEXTURE_MIN_FILTER, GL11_GL_LINEAR);
            GlStateManager._texParameter(GL11_GL_TEXTURE_2D, GL11_GL_TEXTURE_MAG_FILTER, GL11_GL_LINEAR);
            GlStateManager._texImage2D(GL11_GL_TEXTURE_2D, 0, GL11_GL_RGBA, width, height, 0, GL11_GL_RGBA, GL11_GL_BYTE, buffer.asIntBuffer());
            GlStateManager._glFramebufferTexture2D(GL30_GL_FRAMEBUFFER, GL30_GL_COLOR_ATTACHMENT0, GL11_GL_TEXTURE_2D, fboTextureId, 0);

            int rboId = GlStateManager.glGenRenderbuffers();

            GlStateManager._glBindRenderbuffer(GL30_GL_RENDERBUFFER, rboId);
            GlStateManager._glRenderbufferStorage(GL30_GL_RENDERBUFFER, GL14_GL_DEPTH_COMPONENT24, width, height);
            GlStateManager._glFramebufferRenderbuffer(GL30_GL_FRAMEBUFFER, GL30_GL_DEPTH_ATTACHMENT, GL30_GL_RENDERBUFFER, rboId);
            GlStateManager._glBindRenderbuffer(GL30_GL_DRAW_FRAMEBUFFER, 0);

            checkFramebufferStatus();

            GlStateManager._glBindRenderbuffer(GL30_GL_DRAW_FRAMEBUFFER, previousFboId);
            GlStateManager._bindTexture(0);
        }

        public static void checkFramebufferStatus() {
            int status = GlStateManager.glCheckFramebufferStatus(GL30_GL_FRAMEBUFFER);

            if (status == GL30_GL_FRAMEBUFFER_COMPLETE) return;

            switch (status) {
                case GL30_GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
                case GL30_GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
                case GL30_GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
                case GL30_GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
                default -> throw new RuntimeException("glCheckFramebufferStatus returned unknown status: " + status);
            }
        }

        public static void bindFramebuffer() {
            previousFboId = GlStateManager._getInteger(GL30_GL_FRAMEBUFFER_BINDING);
            previousFboIdRead = GlStateManager._getInteger(GL30_GL_READ_FRAMEBUFFER_BINDING);
            previousFboIdDraw = GlStateManager._getInteger(GL30_GL_FRAMEBUFFER_BINDING);

            GlStateManager._glBindFramebuffer(GL30_GL_FRAMEBUFFER, fboId);
            GlStateManager._glBindFramebuffer(GL30_GL_READ_FRAMEBUFFER, fboId);
            GlStateManager._glBindFramebuffer(GL30_GL_DRAW_FRAMEBUFFER, fboId);
        }

        public static void unbindFramebuffer() {
            GlStateManager._glBindFramebuffer(GL30_GL_FRAMEBUFFER, previousFboId);
            GlStateManager._glBindFramebuffer(GL30_GL_READ_FRAMEBUFFER, previousFboIdRead);
            GlStateManager._glBindFramebuffer(GL30_GL_DRAW_FRAMEBUFFER, previousFboIdDraw);
        }

        public static void setMap(float x, float y, int imageSize) {
            float scale = imageSize / 4.0f;

            ldrawthree(x - scale, y + scale, 1.0, 0.0f, 1.0f);
            ldrawthree(x + scale, y + scale, 1.0, 1.0f, 1.0f);
            ldrawthree(x + scale, y - scale, 1.0, 1.0f, 0.0f);
            ldrawthree(x - scale, y - scale, 1.0, 0.0f, 0.0f);
        }

        public static void setMap(Sprite icon, float x, float y, float imageSize) {
            float half = imageSize / 4.0f;

            ldrawthree(x - half, y + half, 1.0, icon.getMinU(), icon.getMaxV());
            ldrawthree(x + half, y + half, 1.0, icon.getMaxU(), icon.getMaxV());
            ldrawthree(x + half, y - half, 1.0, icon.getMaxU(), icon.getMinV());
            ldrawthree(x - half, y - half, 1.0, icon.getMinU(), icon.getMinV());
        }

        public static int tex(BufferedImage image) {
            int glId = TextureUtil.generateTextureId();
            int width = image.getWidth();
            int height = image.getHeight();
            int[] data = new int[width * height];

            image.getRGB(0, 0, width, height, data, 0, width);
            RenderSystem.bindTexture(glId);

            DATA_BUFFER.clear();
            DATA_BUFFER.put(data, 0, width * height);
            DATA_BUFFER.position(0).limit(width * height);

            RenderSystem.texParameter(GL11_GL_TEXTURE_2D, GL11_GL_TEXTURE_MIN_FILTER, GL11_GL_LINEAR);
            RenderSystem.texParameter(GL11_GL_TEXTURE_2D, GL11_GL_TEXTURE_MAG_FILTER, GL11_GL_LINEAR);
            RenderSystem.pixelStore(GL11_GL_UNPACK_ROW_LENGTH, 0);
            RenderSystem.pixelStore(GL11_GL_UNPACK_SKIP_PIXELS, 0);
            RenderSystem.pixelStore(GL11_GL_UNPACK_SKIP_ROWS, 0);
            GlStateManager._texImage2D(GL11_GL_TEXTURE_2D, 0, GL11_GL_RGBA, width, height, 0, GL12_GL_BGRA, GL12_GL_UNSIGNED_INT_8_8_8_8_REV, DATA_BUFFER);

            return glId;
        }

        public static void img(Identifier param) { textureManager.bindTexture(param); }

        public static void register(Identifier resource, AbstractTexture image) { textureManager.registerTexture(resource, image); }

        @NotNull
        public static NativeImage nativeImageFromBufferedImage(BufferedImage image) {
            int glId = tex(image);
            NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);
            RenderSystem.bindTexture(glId);
            nativeImage.loadFromTextureImage(0, false);

            return nativeImage;
        }

        public static void drawPre() { VERTEX_BUFFER.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE); }

        public static void drawPost() { TESSELLATOR.draw(); }

        public static void ldrawone(int x, int y, double z, float u, float v) { VERTEX_BUFFER.vertex(x, y, z).texture(u, v).next(); }

        public static void ldrawthree(double x, double y, double z, float u, float v) { VERTEX_BUFFER.vertex(x, y, z).texture(u, v).next(); }
    }
}