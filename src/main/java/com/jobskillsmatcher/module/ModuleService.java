package com.jobskillsmatcher.module;

import com.jobskillsmatcher.module.port.rest.ModuleView;

import java.util.List;

public interface ModuleService {

    List<ModuleView> listAll();
}
