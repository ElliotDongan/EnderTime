package com.mojang.realmsclient.client;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.worldupload.RealmsUploadCanceledException;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.gui.screens.UploadResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.User;
import net.minecraft.util.LenientJsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class FileUpload {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_RETRIES = 5;
    private static final String UPLOAD_PATH = "/upload";
    private final File file;
    private final long realmId;
    private final int slotId;
    private final UploadInfo uploadInfo;
    private final String sessionId;
    private final String username;
    private final String clientVersion;
    private final String worldVersion;
    private final UploadStatus uploadStatus;
    final AtomicBoolean cancelled = new AtomicBoolean(false);
    @Nullable
    private CompletableFuture<UploadResult> uploadTask;
    private final RequestConfig requestConfig = RequestConfig.custom()
        .setSocketTimeout((int)TimeUnit.MINUTES.toMillis(10L))
        .setConnectTimeout((int)TimeUnit.SECONDS.toMillis(15L))
        .build();

    public FileUpload(File p_87071_, long p_87072_, int p_87073_, UploadInfo p_87074_, User p_87075_, String p_87076_, String p_335512_, UploadStatus p_87077_) {
        this.file = p_87071_;
        this.realmId = p_87072_;
        this.slotId = p_87073_;
        this.uploadInfo = p_87074_;
        this.sessionId = p_87075_.getSessionId();
        this.username = p_87075_.getName();
        this.clientVersion = p_87076_;
        this.worldVersion = p_335512_;
        this.uploadStatus = p_87077_;
    }

    public UploadResult upload() {
        if (this.uploadTask != null) {
            return new UploadResult.Builder().build();
        } else {
            this.uploadTask = CompletableFuture.supplyAsync(() -> this.requestUpload(0), Util.backgroundExecutor());
            if (this.cancelled.get()) {
                this.cancel();
                return new UploadResult.Builder().build();
            } else {
                return this.uploadTask.join();
            }
        }
    }

    public void cancel() {
        this.cancelled.set(true);
    }

    private UploadResult requestUpload(int p_87080_) {
        UploadResult.Builder uploadresult$builder = new UploadResult.Builder();
        if (this.cancelled.get()) {
            return uploadresult$builder.build();
        } else {
            this.uploadStatus.setTotalBytes(this.file.length());
            HttpPost httppost = new HttpPost(this.uploadInfo.getUploadEndpoint().resolve("/upload/" + this.realmId + "/" + this.slotId));
            CloseableHttpClient closeablehttpclient = HttpClientBuilder.create().setDefaultRequestConfig(this.requestConfig).build();

            UploadResult uploadresult;
            try {
                this.setupRequest(httppost);
                HttpResponse httpresponse = closeablehttpclient.execute(httppost);
                long i = this.getRetryDelaySeconds(httpresponse);
                if (!this.shouldRetry(i, p_87080_)) {
                    this.handleResponse(httpresponse, uploadresult$builder);
                    return uploadresult$builder.build();
                }

                uploadresult = this.retryUploadAfter(i, p_87080_);
            } catch (Exception exception) {
                if (!this.cancelled.get()) {
                    LOGGER.error("Caught exception while uploading: ", (Throwable)exception);
                    return uploadresult$builder.build();
                }

                throw new RealmsUploadCanceledException();
            } finally {
                this.cleanup(httppost, closeablehttpclient);
            }

            return uploadresult;
        }
    }

    private void cleanup(HttpPost p_87094_, @Nullable CloseableHttpClient p_87095_) {
        p_87094_.releaseConnection();
        if (p_87095_ != null) {
            try {
                p_87095_.close();
            } catch (IOException ioexception) {
                LOGGER.error("Failed to close Realms upload client");
            }
        }
    }

    private void setupRequest(HttpPost p_87092_) throws FileNotFoundException {
        p_87092_.setHeader(
            "Cookie",
            "sid="
                + this.sessionId
                + ";token="
                + this.uploadInfo.getToken()
                + ";user="
                + this.username
                + ";version="
                + this.clientVersion
                + ";worldVersion="
                + this.worldVersion
        );
        FileUpload.CustomInputStreamEntity fileupload$custominputstreamentity = new FileUpload.CustomInputStreamEntity(
            new FileInputStream(this.file), this.file.length(), this.uploadStatus
        );
        fileupload$custominputstreamentity.setContentType("application/octet-stream");
        p_87092_.setEntity(fileupload$custominputstreamentity);
    }

    private void handleResponse(HttpResponse p_87089_, UploadResult.Builder p_87090_) throws IOException {
        int i = p_87089_.getStatusLine().getStatusCode();
        if (i == 401) {
            LOGGER.debug("Realms server returned 401: {}", p_87089_.getFirstHeader("WWW-Authenticate"));
        }

        p_87090_.withStatusCode(i);
        if (p_87089_.getEntity() != null) {
            String s = EntityUtils.toString(p_87089_.getEntity(), "UTF-8");
            if (s != null) {
                try {
                    JsonElement jsonelement = LenientJsonParser.parse(s).getAsJsonObject().get("errorMsg");
                    Optional<String> optional = Optional.ofNullable(jsonelement).map(JsonElement::getAsString);
                    p_87090_.withErrorMessage(optional.orElse(null));
                } catch (Exception exception) {
                }
            }
        }
    }

    private boolean shouldRetry(long p_87082_, int p_87083_) {
        return p_87082_ > 0L && p_87083_ + 1 < 5;
    }

    private UploadResult retryUploadAfter(long p_87098_, int p_87099_) throws InterruptedException {
        Thread.sleep(Duration.ofSeconds(p_87098_).toMillis());
        return this.requestUpload(p_87099_ + 1);
    }

    private long getRetryDelaySeconds(HttpResponse p_87087_) {
        return Optional.ofNullable(p_87087_.getFirstHeader("Retry-After")).map(NameValuePair::getValue).map(Long::valueOf).orElse(0L);
    }

    public boolean isFinished() {
        return this.uploadTask.isDone() || this.uploadTask.isCancelled();
    }

    @OnlyIn(Dist.CLIENT)
    class CustomInputStreamEntity extends InputStreamEntity {
        private final long length;
        private final InputStream content;
        private final UploadStatus uploadStatus;

        public CustomInputStreamEntity(final InputStream p_87105_, final long p_87106_, final UploadStatus p_87107_) {
            super(p_87105_);
            this.content = p_87105_;
            this.length = p_87106_;
            this.uploadStatus = p_87107_;
        }

        @Override
        public void writeTo(OutputStream p_87109_) throws IOException {
            Args.notNull(p_87109_, "Output stream");

            try (InputStream inputstream = this.content) {
                byte[] abyte = new byte[4096];
                int j;
                if (this.length < 0L) {
                    while ((j = inputstream.read(abyte)) != -1) {
                        if (FileUpload.this.cancelled.get()) {
                            throw new RealmsUploadCanceledException();
                        }

                        p_87109_.write(abyte, 0, j);
                        this.uploadStatus.onWrite(j);
                    }
                } else {
                    long i = this.length;

                    while (i > 0L) {
                        j = inputstream.read(abyte, 0, (int)Math.min(4096L, i));
                        if (j == -1) {
                            break;
                        }

                        if (FileUpload.this.cancelled.get()) {
                            throw new RealmsUploadCanceledException();
                        }

                        p_87109_.write(abyte, 0, j);
                        this.uploadStatus.onWrite(j);
                        i -= j;
                        p_87109_.flush();
                    }
                }
            }
        }
    }
}