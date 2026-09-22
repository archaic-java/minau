package work.archaic.minau;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import work.archaic.service.test.v02.TestTrail;

/** Evidence owned by one executing case; never shared with application diagnostics. */
final class CaseTrail implements TestTrail {
  private static final int MAX_NOTES = 256;
  private static final int MAX_LENGTH = 2048;
  private final Thread owner = Thread.currentThread();
  private final ArrayDeque<String> notes = new ArrayDeque<>();
  private boolean active = true;
  private long omitted;
  private long truncated;

  @Override
  public void note(String message) {
    if (Thread.currentThread() != owner || !active) {
      throw new IllegalStateException("Test trail is only valid on its active case thread");
    }
    Objects.requireNonNull(message, "message");
    if (message.length() > MAX_LENGTH) {
      int end = MAX_LENGTH;
      if (Character.isHighSurrogate(message.charAt(end - 1))
          && Character.isLowSurrogate(message.charAt(end))) {
        end--;
      }
      message = message.substring(0, end);
      truncated++;
    }
    if (notes.size() == MAX_NOTES) {
      notes.removeFirst();
      omitted++;
    }
    notes.addLast(message);
  }

  Evidence finish(boolean failed) {
    active = false;
    var evidence = failed
        ? new Evidence(List.copyOf(notes), omitted, truncated)
        : new Evidence(List.of(), 0, 0);
    notes.clear();
    return evidence;
  }

  record Evidence(List<String> notes, long omitted, long truncated) {}
}
