package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.entity.oj.ProblemSet;
import cn.edu.zjnu.acm.entity.oj.Tag;
import cn.edu.zjnu.acm.repo.problem.*;
import cn.edu.zjnu.acm.repo.user.UserProblemRepository;
import cn.edu.zjnu.acm.util.PageHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.*;

@Slf4j
@Service
public class ProblemSetService {
    private final ProblemSetRepository problemSetRepository;
    private final ProblemService problemService;
    private final TagRepository tagRepository;

    public ProblemSetService(ProblemSetRepository problemSetRepository, TagRepository tagRepository, ProblemService problemService) {
        this.problemSetRepository = problemSetRepository;
        this.tagRepository = tagRepository;
        this.problemService = problemService;
    }

    public Page<ProblemSet> getAllActiveProblemSet(int page, int size) {
        return problemSetRepository.findProblemSetByActive(PageRequest.of(page, size), true);
    }

    public Page<ProblemSet> getAllProblemSet(int page, int size, String search) {
        return problemSetRepository.findAllByTitleContaining(PageRequest.of(page, size), search);
    }

    public Page<ProblemSet> getAllProblemSet(int page, int size, String search, User user) {
        return problemSetRepository.findAllByCreatorAndTitleContaining(PageRequest.of(page, size), user, search);
    }

    public List<ProblemSet> getAllProblemSetByCreator(User user) {
        return problemSetRepository.findAllByCreator(user);
    }

    public List<ProblemSet> getAllActiveProblemSetOrCreator(User user) {
        return problemSetRepository.findAllByCreatorOrActive(user, true);
    }

    public List<ProblemSet> getActiveProblemSetNotOfUser(User user) {
        return problemSetRepository.findAllByActiveAndCreatorNot( true, user);
    }

    public Page<ProblemSet> getByTagName(int page, int size, List<String> tagNames, List<ProblemSet> problemSet) {
        problemSet = new ArrayList<>(problemSet);
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName).orElse(null);
            if (tag == null) {
                continue;
            }
            List<ProblemSet> _tags = problemSetRepository.findAllByTags(tag);
            problemSet.retainAll(_tags);
        }
        page = Math.min((problemSet.size() - 1) / size, page);
        return new PageHolder<ProblemSet>(problemSet, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
    }

    public ProblemSet insertNewProblemSet(ProblemSet problemSet) {
        return problemSetRepository.save(problemSet);
    }

    public Boolean isProblemSetRepeated(String title) {
        return problemSetRepository.findByTitle(title).isPresent();
    }

    public ProblemSet getProblemSetById(Long id) {
        return problemSetRepository.findById(id).orElse(null);
    }

    public ProblemSet getActiveProblemById(Long id) {
        return problemSetRepository.findProblemSetByIdAndActive(id, true).orElse(null);
    }

    public Page<ProblemSet> searchActiveProblemSet(int page, int size, String search, boolean allInOnePage) {
        List<ProblemSet> problemSets = new LinkedList<>();
        try {
            problemSets.addAll(problemSetRepository.findAllByActiveAndTitleContaining(true, search));
        } catch (Exception e) {
            log.debug("search problem by title failed");
        }
        HashSet<ProblemSet> set = new HashSet<>(problemSets);
        List<ProblemSet> _problemSets = new ArrayList<>(set);
        if (allInOnePage) {
            size = Math.max(_problemSets.size(), 1);
            page = 0;
        }
        return new PageHolder<>(_problemSets, PageRequest.of(page, size));
    }

    public Set<Problem> getProblemArrayByIds(Set<Long> ids) {
        Set<Problem> problems = new HashSet<>();
        for (Long id : ids){
            problems.add(problemService.getProblemById(id));
        }
        return problems;
    }

}
