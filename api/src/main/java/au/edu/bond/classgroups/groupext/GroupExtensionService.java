package au.edu.bond.classgroups.groupext;

import au.edu.bond.classgroups.service.Cleanable;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutionException;

/**
 * Created by Shane Argo on 16/06/2014.
 */
public class GroupExtensionService implements Cleanable {

    @Autowired
    private GroupExtensionDAO groupExtensionDAO;

    private LoadingCache<String, GroupExtension> byIdCache;

    public GroupExtensionService(String byIdCacheSpec) {
        byIdCache = CacheBuilder.from(byIdCacheSpec).build(new CacheLoader<String, GroupExtension>() {
            @Override
            public GroupExtension load(String key) throws Exception {
                return groupExtensionDAO.getByExternalSystemId(key);
            }
        });
    }

    public GroupExtension getGroupExtensionByExternalId(String externalId) throws ExecutionException {
        return byIdCache.get(externalId);
    }

    public void create(GroupExtension groupExtension) throws RuntimeException {
        groupExtensionDAO.create(groupExtension);
        byIdCache.put(groupExtension.getExternalSystemId(), groupExtension);
    }

    public void update(GroupExtension groupExtension, long courseId) {
        groupExtensionDAO.update(groupExtension);
        byIdCache.put(groupExtension.getExternalSystemId(), groupExtension);
    }

    public void delete(GroupExtension groupExtension, long courseId) {
        groupExtensionDAO.delete(groupExtension);
        byIdCache.invalidate(groupExtension.getExternalSystemId());
    }

    public synchronized void clearCaches() {
        byIdCache.invalidateAll();
        byIdCache.cleanUp();
    }

    public GroupExtensionDAO getGroupExtensionDAO() {
        return groupExtensionDAO;
    }

    public void setGroupExtensionDAO(GroupExtensionDAO groupExtensionDAO) {
        this.groupExtensionDAO = groupExtensionDAO;
    }
}
