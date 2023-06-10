package common;

import com.proxy.BinarySearcher;
import com.proxy.ILogService;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class HeaderProcessing {
    final byte[] crlf = {'\r', '\n'};
    final byte[] crlfcrlf = {'\r', '\n', '\r', '\n'};
    final byte[] colonSpace = {':', ' '};
    final private Queue<ByteBuffer> bufferQueue = new ConcurrentLinkedQueue<>();
    public final int BUFFER_SIZE = 5000;
    protected ILogService logService;

    protected HeaderProcessing(ILogService logService) {
        this.logService = logService;
    }

    protected String getHeader(@NotNull ByteBuffer byteBuffer, @NotNull String key) {
        String retVal = "";
        try {
            BinarySearcher bs = new BinarySearcher();
            // Check that the double CRLF is present
            List<Integer> indexList = bs.searchBytes(byteBuffer.array(), crlfcrlf, 0, byteBuffer.limit());
            if (indexList.size() > 0) {
                final int endOfHeadersIdx = indexList.get(0)+crlfcrlf.length-1;
                // OK so look for the header key
                indexList = bs.searchBytes(byteBuffer.array(), key.getBytes(StandardCharsets.UTF_8), 0, endOfHeadersIdx);
                if (indexList.size() > 0) {
                    final int idx1 = indexList.get(0);
                    // Find the CRLF at the end of this header
                    indexList = bs.searchBytes(byteBuffer.array(), crlf, idx1, endOfHeadersIdx);
                    if (indexList.size() > 0) {
                        final int endIdx = indexList.get(0);
                        //Find the start of the header value
                        indexList = bs.searchBytes(byteBuffer.array(), colonSpace, idx1, endIdx);
                        if (indexList.size() == 1) {
                            final int startIdx = indexList.get(0) + colonSpace.length;
                            retVal = new String(byteBuffer.array(), startIdx, endIdx - startIdx);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logService.getCam().error(ex.getClass().getName() + " in getHeader: " + ex.getMessage());
        }
        return retVal;
    }
    protected boolean addHeader(@NotNull ByteBuffer src, AtomicReference<ByteBuffer> arDest, @NotNull String key, @NotNull String value) {
        boolean retVal = false;
        // Check if the header is already present
        if(!getHeader(src, key).equals(value)) {
            final ByteBuffer srcClone = getBuffer();
            srcClone.put(src.array(), 0, src.limit());
            srcClone.flip();
            ByteBuffer dest = getBuffer();
            BinarySearcher bs = new BinarySearcher();
            // Find the first CRLF in the source buffer
            List<Integer> indexList = bs.searchBytes(srcClone.array(), crlf, 0, srcClone.limit());
            if (indexList.size() > 0) {
                final int idx1 = indexList.get(0) + crlf.length;
                // Copy up to just after the first crlf to the dest buffer
                dest.put(srcClone.array(), 0, idx1);
                // Append the new header to follow this
                dest.put(key.getBytes());
                dest.put(colonSpace);
                dest.put(value.getBytes());
                dest.put(crlf);
                // Append the remainder of the source buffer to follow this
                dest.put(srcClone.array(), idx1, srcClone.limit() - idx1);
                dest.flip();
                arDest.set(dest);
                recycle(srcClone);
                retVal = true;
                recycle(src);
            }
        }
        else {
            arDest.set(src);
            retVal = true; // Header already present, return success
        }
        return retVal;
    }

    protected boolean removeHeader(@NotNull ByteBuffer src, AtomicReference<ByteBuffer> arDest, @NotNull String key) {
        boolean retVal = false;
        BinarySearcher bs = new BinarySearcher();
        // Find the first CRLF in the source buffer
        List<Integer> indexList = bs.searchBytes(src.array(), key.getBytes(), 0, src.limit());
        if (indexList.size() > 0) {
            final int startIdx = indexList.get(0);
            indexList = bs.searchBytes(src.array(), crlf, startIdx, src.limit());
            if (indexList.size() > 0) {
                final int endIdx = indexList.get(0) + crlf.length;
                final ByteBuffer dest = getBuffer();
                dest.put(src.array(), 0, startIdx);
                dest.put(src.array(), endIdx, src.limit() - endIdx);
                dest.flip();
                arDest.set(dest);
                retVal = true;
                recycle(src);
            }
        }
        return retVal;
    }

    protected boolean modifyHeader(@NotNull ByteBuffer src, AtomicReference<ByteBuffer> arDest, @NotNull String key, @NotNull String newValue) {
        boolean retVal = false;
        final ByteBuffer srcClone = getBuffer();
        srcClone.put(src.array(), 0, src.limit());
        srcClone.flip();
        AtomicReference<ByteBuffer> headerRemoved = new AtomicReference<>();
        // First remove the existing header
        if (removeHeader(srcClone, headerRemoved, key)) {
            // Then add with the required new value
            retVal = addHeader(headerRemoved.get(), arDest, key, newValue);
            if(!retVal)
                recycle(headerRemoved.get());
        }
        else
            recycle(srcClone);

        //   System.out.print(new String(headerRemoved.get().array(), 0, headerRemoved.get().limit()));
        return retVal;
    }

    protected String getHTTPHeader(@NotNull ByteBuffer byteBuffer) {
        String httpHeader = "";
        // Check there is a double CRLF
        BinarySearcher bs = new BinarySearcher();
        List<Integer> indexList = bs.searchBytes(byteBuffer.array(), crlfcrlf, 0, byteBuffer.limit());
        if (indexList.size() > 0) {
            final int endOfHeadersIdx = indexList.get(0)+crlfcrlf.length-1;
            // Find the first crlf
            indexList = bs.searchBytes(byteBuffer.array(), crlf, 0, endOfHeadersIdx);
            if (indexList.size() > 0) {
                String firstLine = new String(byteBuffer.array(), 0, endOfHeadersIdx);
                if (firstLine.contains("HTTP"))
                    httpHeader = firstLine;
            }
        }

        return httpHeader;
    }


    protected String getRTSPHeader(@NotNull ByteBuffer byteBuffer) {
        String rtspHeader = "";
        // Check there is a double CRLF
        BinarySearcher bs = new BinarySearcher();
        List<Integer> indexList = bs.searchBytes(byteBuffer.array(), crlfcrlf, 0, byteBuffer.limit());
        if (indexList.size() > 0) {
            final int endOfHeadersIdx = indexList.get(0)+crlfcrlf.length-1;
            // Find the first crlf
            indexList = bs.searchBytes(byteBuffer.array(), crlf, 0, endOfHeadersIdx);
            if (indexList.size() > 0) {
                String firstLine = new String(byteBuffer.array(), 0, indexList.get(0));
                if (firstLine.contains("RTSP/"))
                    rtspHeader = firstLine;
            }
        }

        return rtspHeader;
    }

    protected String getRTSPMethod(@NotNull ByteBuffer byteBuffer) {
        String retVal = "";
        final String header = getRTSPHeader(byteBuffer);
        final int idxOfSpace = header.indexOf(" ");
        if(idxOfSpace != -1) {
            retVal = header.substring(0, idxOfSpace-1);
        }
        return retVal;
    }

    protected String getRTSPUri(@NotNull ByteBuffer byteBuffer) {
        String retVal = "";
        final String header = getRTSPHeader(byteBuffer);
        final int startIdx = header.indexOf(" ")+1;
        if(startIdx > 0) {
            final int endIdx = header.indexOf(" ", startIdx);
            if(endIdx != -1) {
                retVal = header.substring(startIdx, endIdx-1);
            }
        }
        return retVal;
    }

    /**
     * getBuffer: Get a new ByteBuffer of BUFFER_SIZE bytes length.
     *
     * @return: The buffer
     */
    public ByteBuffer getBuffer() {
        ByteBuffer buf = Objects.requireNonNullElseGet(bufferQueue.poll(), () -> ByteBuffer.allocate(BUFFER_SIZE));
        buf.clear();
        return buf;
    }

    public synchronized void recycle(ByteBuffer buf) {
        buf.clear();
        bufferQueue.add(buf);
    }
}
