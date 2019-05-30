package au.edu.bond.classgroups.service;

import au.edu.bond.classgroups.dao.BbGroupDAO;
import blackboard.data.ValidationException;
import blackboard.data.course.Group;
import blackboard.persist.Id;
import blackboard.persist.PersistenceException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * Created by Shane Argo on 16/06/2014.
 */
public class BbGroupService implements Cleanable {

    @Autowired
    private BbGroupDAO bbGroupDAO;

    private final LoadingCache</*Course Id*/Id, ConcurrentMap</*Group Title*/String, Group>> byTitleCache;
    private final LoadingCache</*Course Id*/Id, ConcurrentMap</*Group Id*/Id, Group>> byIdCache;
    private final LoadingCache</*Group Id*/Id, /*Course Id*/Id> courseIdCache;

    public BbGroupService(String byIdCacheSpec, String byTitleCacheSpec, String courseIdCacheSpec) {
        byIdCache = CacheBuilder.from(byIdCacheSpec).build(new CacheLoader<Id, ConcurrentMap<Id, Group>>() {
            @Override
            public ConcurrentMap<Id, Group> load(Id courseId) throws Exception {
                Collection<Group> classGroups = bbGroupDAO.getByCourseId(courseId);
                ConcurrentHashMap<Id, Group> groupMap = new ConcurrentHashMap<>();
                for (Group group : classGroups) {
                    // populate the group's tools by using the getAvailableTools() function
                    group.getAvailableTools();
                    groupMap.put(group.getId(), group);
                }
                return groupMap;
            }
        });

        byTitleCache = CacheBuilder.from(byTitleCacheSpec).build(new CacheLoader<Id, ConcurrentMap<String, Group>>() {
            @Override
            public ConcurrentMap<String, Group> load(Id courseId) throws Exception {
                Map<Id, Group> groupMap = byIdCache.get(courseId);
                ConcurrentHashMap<String, Group> titleMap = new ConcurrentHashMap<>();
                for (Group group : groupMap.values()) {
                    if(group != null && group.getTitle() != null && !group.getTitle().isEmpty()) {
                        titleMap.put(group.getTitle(), group);
                    }
                }
                return titleMap;
            }
        });

        courseIdCache = CacheBuilder.from(courseIdCacheSpec).build(new CacheLoader<Id, Id>() {
            @Override
            public Id load(Id groupId) throws Exception {
                return bbGroupDAO.getById(groupId).getCourseId();
            }
        });
    }

    public Group getById(long id, Id courseId) throws ExecutionException {
        return getById(getIdFromLong(id), courseId);
    }

    private Group getById(Id groupId, Id courseId) throws ExecutionException {
        return byIdCache.get(courseId).get(groupId);
    }

    public Group getByTitleAndCourseId(String title, Id courseId) throws ExecutionException {
        if(title == null || title.isEmpty()) {
            return null;
        }
        return byTitleCache.get(courseId).get(title);
    }

    public synchronized void createOrUpdate (Group group) throws ExecutionException, PersistenceException, ValidationException {
        bbGroupDAO.createOrUpdate(group);

        addGroupToCache(group);
    }

    public synchronized void createOrUpdate(Group group, Set<Id> courseMembershipIds) throws ExecutionException, PersistenceException, ValidationException {
        bbGroupDAO.createOrUpdate(group, courseMembershipIds);
        addGroupToCache(group);
    }

    private void addGroupToCache(Group group) throws ExecutionException {
        Id courseId = group.getCourseId();
        Map<Id, Group> groupMap = byIdCache.get(courseId);
        Map<String, Group> titleMap = byTitleCache.get(courseId);

        groupMap.put(group.getId(), group);
        titleMap.put(group.getTitle(), group);
    }

    public Id getIdFromLong(Long id) {
        return Id.toId(Group.DATA_TYPE, id);
    }

    public synchronized void clearCaches() {
        byTitleCache.invalidateAll();
        byTitleCache.cleanUp();
        byIdCache.invalidateAll();
        byIdCache.cleanUp();
    }
}
