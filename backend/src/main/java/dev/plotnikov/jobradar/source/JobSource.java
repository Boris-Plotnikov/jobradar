package dev.plotnikov.jobradar.source;

import java.util.List;

public interface JobSource {
    String name();

    boolean isEnabled();

    List<RawJob> fetch();
}
