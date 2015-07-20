package me.rbrickis.ssuploader.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.rbrickis.aurora.http.HttpClient;
import me.rbrickis.aurora.http.HttpResponse;
import me.rbrickis.aurora.http.RequestType;
import me.rbrickis.aurora.http.impl.builder.HttpClientBuilder;
import me.rbrickis.aurora.http.impl.builder.HttpParameterBuilder;
import me.rbrickis.ssuploader.SSUploader;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class ScreenShotHelper {

    /**
     * A buffer to hold pixel values returned by OpenGL.
     */
    private static IntBuffer pixelBuffer;
    /**
     * The built-up array that contains all the pixel values returned by OpenGL.
     */
    private static int[] pixelValues;

    private static final Logger logger = LogManager.getLogger();
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    /**
     * Saves a screenshot in the game directory with the given file name (or null to generate a time-stamped name).
     * Args: gameDirectory, fileName, requestedWidthInPixels, requestedHeightInPixels, frameBuffer
     */
    public static IChatComponent saveScreenshot(File gameDirectory, String fileName, int requestWidthInPixels, int requestedHeightInPixels, Framebuffer frameBuffer) {
        try {
            File file2 = new File(gameDirectory, "screenshots");
            file2.mkdir();

            if (OpenGlHelper.isFramebufferEnabled()) {
                requestWidthInPixels = frameBuffer.framebufferTextureWidth;
                requestedHeightInPixels = frameBuffer.framebufferTextureHeight;
            }

            int area = requestWidthInPixels * requestedHeightInPixels;

            if (pixelBuffer == null || pixelBuffer.capacity() < area) {
                pixelBuffer = BufferUtils.createIntBuffer(area);
                pixelValues = new int[area];
            }

            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            pixelBuffer.clear();

            if (OpenGlHelper.isFramebufferEnabled()) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, frameBuffer.framebufferTexture);
                GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
            } else {
                GL11.glReadPixels(0, 0, requestWidthInPixels, requestedHeightInPixels, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
            }

            pixelBuffer.get(pixelValues);
            TextureUtil.func_147953_a(pixelValues, requestWidthInPixels, requestedHeightInPixels);
            BufferedImage bufferedimage = null;

            if (OpenGlHelper.isFramebufferEnabled()) {
                bufferedimage = new BufferedImage(frameBuffer.framebufferWidth, frameBuffer.framebufferHeight, 1);
                int l = frameBuffer.framebufferTextureHeight - frameBuffer.framebufferHeight;

                for (int i1 = l; i1 < frameBuffer.framebufferTextureHeight; ++i1) {
                    for (int j1 = 0; j1 < frameBuffer.framebufferWidth; ++j1) {
                        bufferedimage.setRGB(j1, i1 - l, pixelValues[i1 * frameBuffer.framebufferTextureWidth + j1]);
                    }
                }
            } else {
                bufferedimage = new BufferedImage(requestWidthInPixels, requestedHeightInPixels, 1);
                bufferedimage.setRGB(0, 0, requestWidthInPixels, requestedHeightInPixels, pixelValues, 0, requestWidthInPixels);
            }

            File file3;

            if (fileName == null) {
                file3 = getTimestampedPNGFileForDirectory(file2);
            } else {
                file3 = new File(file2, fileName);
            }

            // Write to the file
            ImageIO.write(bufferedimage, "png", file3);

            String fName = file3.getName();

            boolean uploadSuccess = false;

            // Upload!
            if (!SSUploader.client_id.equalsIgnoreCase("none")) {
                try {
                    JsonObject response = uploadImage(file3, SSUploader.client_id, file3.getName(), "");
                    fName = response.get("data").getAsJsonObject().get("link").getAsString();
                    uploadSuccess = true;
                } catch (Exception ex) {
                   /* Ignore, skip the error, we tried uploading but failed :( */
                }
            }

            ChatComponentText chatcomponenttext = new ChatComponentText(fName);
            // if the upload was a success it should be the Imgur link!
            chatcomponenttext.getChatStyle().setChatClickEvent(new ClickEvent(uploadSuccess ? ClickEvent.Action.OPEN_URL
                    : ClickEvent.Action.OPEN_FILE, uploadSuccess ? fName : file3.getAbsolutePath()));
            chatcomponenttext.getChatStyle().setUnderlined(true);
            return new ChatComponentTranslation("screenshot.success", chatcomponenttext);
        } catch (IOException exception) {
            logger.warn("Couldn\'t save screenshot", exception);
            return new ChatComponentTranslation("screenshot.failure", exception.getMessage());
        }
    }

    /**
     * @param file
     * @param client_id
     * @return A JsonObject that contains the link to the image
     * @throws IOException
     */
    public static JsonObject uploadImage(File file, String client_id, String title, String description) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        byte[] bastream = new byte[stream.available()];
        DataInputStream disstream = new DataInputStream(stream);
        disstream.readFully(bastream);

        String base64 = Base64.getEncoder().encodeToString(bastream);

        HttpClient client = new HttpClientBuilder()
                .url("https://api.imgur.com/3/upload")
                .agent("Aurora HttpClient v0.1")
                .property("Authorization", "Client-ID " + client_id)
                .parameters(new HttpParameterBuilder()
                        .addParameter("image", base64)
                        .addParameter("client_id", client_id)
                        .addParameter("type", "base64")
                        .addParameter("title", title)
                        .addParameter("description", description))
                .method(RequestType.POST)
                .build();
        HttpResponse response = client.execute();
        return new Gson().fromJson(response.asString(), JsonObject.class);
    }

    /**
     * Creates a unique PNG file in the given directory named by a timestamp.  Handles cases where the timestamp alone
     * is not enough to create a uniquely named file, though it still might suffer from an unlikely race condition where
     * the filename was unique when this method was called, but another process or thread created a file at the same
     * path immediately after this method returned.
     */
    private static File getTimestampedPNGFileForDirectory(File p_74290_0_) {
        String s = dateFormat.format(new Date()).toString();
        int i = 1;

        while (true) {
            File file2 = new File(p_74290_0_, s + (i == 1 ? "" : "_" + i) + ".png");

            if (!file2.exists()) {
                return file2;
            }

            ++i;
        }
    }
}
