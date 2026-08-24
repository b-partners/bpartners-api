package app.bpartners.api.concurrency;

import static app.bpartners.api.concurrency.ThreadRenamer.getRandomSubThreadNamePrefixFrom;
import static app.bpartners.api.concurrency.ThreadRenamer.renameThread;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;

import app.bpartners.api.PojaGenerated;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@PojaGenerated
@SuppressWarnings("all")
@Component
public class Workers<T> implements Function<List<Callable<T>>, List<T>> {
  private final ExecutorService executorService;

  public Workers() {
    this.executorService = newVirtualThreadPerTaskExecutor();
  }

  @Override
  public List<T> apply(List<Callable<T>> callables) {
    var parentThread = currentThread();
    callables =
        callables.stream()
            .map(
                c ->
                    (Callable<T>)
                        () -> {
                          renameThread(
                              parentThread, getRandomSubThreadNamePrefixFrom(parentThread));
                          return c.call();
                        })
            .toList();
    List<Future<T>> futures;
    try {
      futures = executorService.invokeAll(callables);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return futures.stream().map(this::handleFutureException).toList();
  }

  private T handleFutureException(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
