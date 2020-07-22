package nl.knaw.huc.resources;

import nl.knaw.huc.exceptions.PayloadTooLargeException;

import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.io.InputStream;

public class ResourceUtils {
  public static byte[] readContents(InputStream uploadedInputStream, int maxFileSize) {
    if (uploadedInputStream == null) {
      throw new BadRequestException("File is missing");
    }

    try {
      return new InputStreamLimiter(uploadedInputStream, maxFileSize).readAllBytes();
    } catch (IOException e) {
      throw new BadRequestException("Could not read input stream of posted file", e);
    }
  }

  private static class InputStreamLimiter extends InputStream {
    private final InputStream delegate;
    private final int maxAllowedSize;
    private int numRead;

    private InputStreamLimiter(InputStream delegate, int maxAllowedSize) {
      this.delegate = delegate;
      this.maxAllowedSize = maxAllowedSize;
    }

    @Override
    public int read() throws IOException {
      final int c = delegate.read();

      if (c == -1) { // No limit on reading EOF
        return -1;
      }

      if (++numRead > maxAllowedSize) {
        throw new PayloadTooLargeException("max. allowed size: " + maxAllowedSize);
      }

      return c;
    }
  }

}
