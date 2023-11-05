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
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.image.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class OpenGL {
    public static final int
            GL11_GL_DEPTH_BUFFER_BIT     = 0x100,
            GL11_GL_LEQUAL               = 0x203,
            GL11_GL_ALWAYS               = 0x207,
            GL11_GL_SRC_ALPHA            = 0x302,
            GL11_GL_ONE_MINUS_SRC_ALPHA  = 0x303,
            GL11_GL_DST_COLOR            = 0x306,
            GL11_GL_CULL_FACE            = 0xB44,
            GL11_GL_DEPTH_TEST           = 0xB71,
            GL11_GL_BLEND                = 0xBE2,
            GL11_GL_SCISSOR_TEST         = 0xC11,
            GL11_GL_COLOR_CLEAR_VALUE    = 0xC22,
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
            GL11_GL_POLYGON_OFFSET_FILL  = 0x8037,
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

    public static void glEnable(int target) {
        switch (target) {
            case GL11_GL_CULL_FACE -> RenderSystem.enableCull();
            case GL11_GL_DEPTH_TEST -> RenderSystem.enableDepthTest();
            case GL11_GL_BLEND -> RenderSystem.enableBlend();
            case GL11_GL_SCISSOR_TEST -> GL11.glEnable(GL11_GL_SCISSOR_TEST);
            case GL11_GL_POLYGON_OFFSET_FILL -> RenderSystem.enablePolygonOffset();
            default -> VoxelConstants.getLogger().warn("OpenGL - Invalid state received by Enable (" + target + ")");
        }
    }

    public static void glDisable(int target) {
        switch (target) {
            case GL11_GL_CULL_FACE -> RenderSystem.disableCull();
            case GL11_GL_DEPTH_TEST -> RenderSystem.disableDepthTest();
            case GL11_GL_BLEND -> RenderSystem.disableBlend();
            case GL11_GL_SCISSOR_TEST -> GL11.glDisable(GL11.GL_SCISSOR_TEST);
            case GL11_GL_POLYGON_OFFSET_FILL -> RenderSystem.disablePolygonOffset();
            default -> VoxelConstants.getLogger().warn("OpenGL - Invalid state received by Disable (" + target + ")");
        }
    }

    public static void glDeleteTexture(int texture) { RenderSystem.deleteTexture(texture); }

    public static void glDepthMask(boolean mask) { RenderSystem.depthMask(mask); }

    public static int glGenTextures() { return GlStateManager._genTexture(); }

    public static int glGetTexLevelParameteri(int target, int level, int pname) { return GlStateManager._getTexLevelParameter(target, level, pname); }

    public static void glPixelStorei(int pname, int param) { RenderSystem.pixelStore(pname, param); }

    public static void glPolygonOffset(float factor, float units) { RenderSystem.polygonOffset(factor, units); }

    public static void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, IntBuffer pixels) { GlStateManager._texImage2D(target, level, internalFormat, width, height, border, format, type, pixels); }

    public static void glTexParameterf(int target, int pname, float param) { GlStateManager._texParameter(target, pname, param); }

    public static void glTexParameteri(int target, int pname, int param) { RenderSystem.texParameter(target, pname, param); }

    public static void glViewport(int x, int y, int width, int height) { RenderSystem.viewport(x, y, width, height); }

    public static void glBindTexture(int target, int texture) {
        if (target == GL11_GL_TEXTURE_2D) {
            RenderSystem.bindTexture(texture);
            return;
        }

        GL11.glBindTexture(target, texture);
    }

    public static void glGenerateMipmap(int target) { GL30.glGenerateMipmap(target); }

    public static void glGetFloatv(int pname, FloatBuffer params) { GL11.glGetFloatv(pname, params); }

    public static int glGetInteger(int pname) { return GL11.glGetInteger(pname); }

    public static void glGetTexImage(int tex, int level, int format, int type, ByteBuffer pixels) { GL11.glGetTexImage(tex, level, format, type, pixels); }

    public static void glGetTexImage(int tex, int level, int format, int type, IntBuffer pixels) { GL11.glGetTexImage(tex, level, format, type, pixels); }

    public static void glPopAttrib() { GL11.glPopAttrib(); }

    public static void glPushAttrib(int mask) { GL11.glPushAttrib(mask); }

    public static void glScissor(int x, int y, int width, int height) { GL11.glScissor(x, y, width, height); }

    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer pixels) { GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels); }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, IntBuffer pixels) { GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels); }

    public static int glGenFramebuffers() { return GL30.glGenFramebuffers(); }

    public static void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) { GL30.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer); }

    public static void glBindFramebuffer(int target, int framebuffer) { GL30.glBindFramebuffer(target, framebuffer); }

    public static void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) { GL30.glFramebufferTexture2D(target, attachment, textarget, texture, level); }

    public static int glGenRenderbuffers() { return GL30.glGenRenderbuffers(); }

    public static void glBindRenderbuffer(int target, int renderbuffer) { GL30.glBindRenderbuffer(target, renderbuffer); }

    public static void glRenderbufferStorage(int target, int internalformat, int width, int height) { GL30.glRenderbufferStorage(target, internalformat, width, height); }

    public static int glCheckFramebufferStatus(int target) { return GL30.glCheckFramebufferStatus(target); }

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
            previousFboId = glGetInteger(GL30_GL_FRAMEBUFFER_BINDING);
            fboId = glGenFramebuffers();
            fboTextureId = glGenTextures();

            int width = 512;
            int height = 512;
            ByteBuffer buffer = BufferUtils.createByteBuffer(4 * width * height);

            glBindFramebuffer(GL30_GL_FRAMEBUFFER, fboId);
            glBindTexture(GL11_GL_TEXTURE_2D, fboTextureId);
            glTexParameteri(GL11_GL_TEXTURE_2D, GL11_GL_TEXTURE_WRAP_S, GL12_GL_CLAMP_TO_EDGE);
            glTexParameterf(GL11_GL_TEXTURE_2D, GL11_GL_TEXTURE_WRAP_T, GL12_GL_CLAMP_TO_EDGE);
            glTexParameterf(GL11_GL_TEXTURE_2D, GL11_GL_TEXTURE_MIN_FILTER, GL11_GL_LINEAR);
            glTexParameterf(GL11_GL_TEXTURE_2D, GL11_GL_TEXTURE_MAG_FILTER, GL11_GL_LINEAR);
            glTexImage2D(GL11_GL_TEXTURE_2D, 0, GL11_GL_RGBA, width, height, 0, GL11_GL_RGBA, GL11_GL_BYTE, buffer);
            glFramebufferTexture2D(GL30_GL_FRAMEBUFFER, GL30_GL_COLOR_ATTACHMENT0, GL11_GL_TEXTURE_2D, fboTextureId, 0);

            int rboId = glGenRenderbuffers();

            glBindRenderbuffer(GL30_GL_RENDERBUFFER, rboId);
            glRenderbufferStorage(GL30_GL_RENDERBUFFER, GL14_GL_DEPTH_COMPONENT24, width, height);
            glFramebufferRenderbuffer(GL30_GL_FRAMEBUFFER, GL30_GL_DEPTH_ATTACHMENT, GL30_GL_RENDERBUFFER, rboId);
            glBindRenderbuffer(GL30_GL_DRAW_FRAMEBUFFER, 0);

            checkFramebufferStatus();

            glBindRenderbuffer(GL30_GL_DRAW_FRAMEBUFFER, previousFboId);
            GlStateManager._bindTexture(0);
        }

        public static void checkFramebufferStatus() {
            int status = glCheckFramebufferStatus(GL30_GL_FRAMEBUFFER);

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
            previousFboId = glGetInteger(GL30_GL_FRAMEBUFFER_BINDING);
            previousFboIdRead = glGetInteger(GL30_GL_READ_FRAMEBUFFER_BINDING);
            previousFboIdDraw = glGetInteger(GL30_GL_FRAMEBUFFER_BINDING);

            glBindFramebuffer(GL30_GL_FRAMEBUFFER, fboId);
            glBindFramebuffer(GL30_GL_READ_FRAMEBUFFER, fboId);
            glBindFramebuffer(GL30_GL_DRAW_FRAMEBUFFER, fboId);
        }

        public static void unbindFramebuffer() {
            glBindFramebuffer(GL30_GL_FRAMEBUFFER, previousFboId);
            glBindFramebuffer(GL30_GL_READ_FRAMEBUFFER, previousFboIdRead);
            glBindFramebuffer(GL30_GL_DRAW_FRAMEBUFFER, previousFboIdDraw);
        }

        public static void setMapWithScale(int x, int y, float scale) { setMap(x, y, (int) (128f * scale)); }

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
            glBindTexture(GL11_GL_TEXTURE_2D, glId);

            DATA_BUFFER.clear();
            DATA_BUFFER.put(data, 0, width * height);
            DATA_BUFFER.position(0).limit(width * height);

            glTexParameteri(GL11_GL_TEXTURE_2D, GL11_GL_TEXTURE_MIN_FILTER, GL11_GL_LINEAR);
            glTexParameteri(GL11_GL_TEXTURE_2D, GL11_GL_TEXTURE_MAG_FILTER, GL11_GL_LINEAR);
            glPixelStorei(GL11_GL_UNPACK_ROW_LENGTH, 0);
            glPixelStorei(GL11_GL_UNPACK_SKIP_PIXELS, 0);
            glPixelStorei(GL11_GL_UNPACK_SKIP_ROWS, 0);
            glTexImage2D(GL11_GL_TEXTURE_2D, 0, GL11_GL_RGBA, width, height, 0, GL12_GL_BGRA, GL12_GL_UNSIGNED_INT_8_8_8_8_REV, DATA_BUFFER);

            return glId;
        }

        public static void img2(String param) { img2(new Identifier(param)); }

        public static void img(Identifier param) { textureManager.bindTexture(param); }

        public static void img2(Identifier param) { RenderSystem.setShaderTexture(0, param); }

        public static void disp(int param) { glBindTexture(GL11_GL_TEXTURE_2D, param); }

        public static void disp2(int param) { RenderSystem.setShaderTexture(0, param); }

        public static void register(Identifier resource, AbstractTexture image) { textureManager.registerTexture(resource, image); }

        @NotNull
        public static NativeImage nativeImageFromBufferedImage(BufferedImage image) {
            int glId = tex(image);
            NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);
            RenderSystem.bindTexture(glId);
            nativeImage.loadFromTextureImage(0, false);

            return nativeImage;
        }

        public static void drawPre() { drawPre(VertexFormats.POSITION_TEXTURE); }

        public static void drawPre(VertexFormat format) { VERTEX_BUFFER.begin(VertexFormat.DrawMode.QUADS, format); }

        public static void drawPost() { TESSELLATOR.draw(); }

        public static void glah(int g) { glDeleteTexture(g); }

        public static void ldrawone(int x, int y, double z, float u, float v) { VERTEX_BUFFER.vertex(x, y, z).texture(u, v).next(); }

        public static void ldrawthree(double x, double y, double z, float u, float v) { VERTEX_BUFFER.vertex(x, y, z).texture(u, v).next(); }
    }
}