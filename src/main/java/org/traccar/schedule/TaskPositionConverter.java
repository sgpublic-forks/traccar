package org.traccar.schedule;

import com.google.inject.Injector;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.geoconv.PositionConverter;

import java.util.concurrent.ScheduledExecutorService;

public class TaskPositionConverter implements ScheduleTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskPositionConverter.class);

    private final Injector injector;

    @Inject
    public TaskPositionConverter(Injector inject) {
        this.injector = inject;
    }

    @Override
    public void schedule(ScheduledExecutorService executor) {
        PositionConverter.all().forEachOrdered(converterClass -> {
            var converter = injector.getInstance(converterClass);
            if (converter.enable()) {
                converter.schedule(executor);
            }
        });
    }

    @Override
    public void run() { }
}
