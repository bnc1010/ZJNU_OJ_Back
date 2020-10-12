package cn.edu.zjnu.acm.common.utils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class List2Page {
    /**
     * pageSize 每页数量以pageable中为准
     * page 从0开始
     *
     * @param list
     * @param pageable
     * @param <T>
     * @return
     */
    public static <T> Page<T> listToPage(List<T> list, Pageable pageable) {
        return getPage(list, pageable);
    }

    /**
     * pageSize 每页数量为15
     * page 从0开始
     *
     * @param list
     * @param page
     * @param <T>
     * @return
     */
    public static <T> Page<T> listToPage(List<T> list, int page) {
        PageRequest pageable = PageRequest.of(page, 15);
        return getPage(list, pageable);
    }

    private static <T> Page<T> getPage(List<T> list, Pageable pageable) {
        //第n页起始值
        int pageStart = pageable.getPageNumber() * pageable.getPageSize();
        //第n页期望结尾值
        int expectPageEnd = pageStart + pageable.getPageSize() - 1;
        return Optional.of(list.size())
                .filter(size -> size >= pageStart)
                .map(size -> getRealSubList(pageStart, expectPageEnd, list, pageable))
                .orElseGet(() -> getEmptySubList(list, pageable));
    }

    private static <T> PageImpl<T> getRealSubList(int pageStart, int expectPageEnd, List<T> list, Pageable pageable) {
        int realPageEnd = (list.size() > expectPageEnd || list.size() == expectPageEnd) ? expectPageEnd + 1 : list.size();
        return new PageImpl<>(list.subList(pageStart, realPageEnd), pageable, list.size());
    }

    private static <T> PageImpl<T> getEmptySubList(List<T> list, Pageable pageable) {
        return new PageImpl<>(new ArrayList(), pageable, list.size());
    }
}