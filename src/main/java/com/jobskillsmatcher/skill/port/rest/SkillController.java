package com.jobskillsmatcher.skill.port.rest;

import com.jobskillsmatcher.skill.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public List<SkillView> search(@RequestParam(value = "query", required = false) String query,
                                  @RequestParam(value = "limit", required = false) Integer limit) {
        return skillService.search(query, limit);
    }
}
