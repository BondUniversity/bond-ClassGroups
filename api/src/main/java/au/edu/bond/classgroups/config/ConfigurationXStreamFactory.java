package au.edu.bond.classgroups.config;

import au.edu.bond.classgroups.model.Schedule;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.ClassAliasingMapper;

/**
 * Created by Shane Argo on 21/12/14.
 */
public class ConfigurationXStreamFactory {

    public static XStream getConfigXStream() {
        XStream xstream = new XStream(new DomDriver("UTF-8"));
        xstream.alias("configuration", Configuration.class);
        xstream.alias("pullFileCsvFeedDeserialiser", PullFileCsvFeedDeserialiserConfig.class);
        xstream.alias("pullUrlCsvFeedDeserialiser", PullUrlCsvFeedDeserialiserConfig.class);
        xstream.alias("pushCsvFeedDeserialiser", PushCsvFeedDeserialiserConfig.class);

        ClassAliasingMapper mapper = new ClassAliasingMapper(xstream.getMapper());
        mapper.addClassAlias("tool", String.class);
        xstream.registerLocalConverter(Configuration.class, "defaultTools", new CollectionConverter(mapper));

        mapper.addClassAlias("schedule", Schedule.class);
        xstream.registerLocalConverter(Configuration.class, "schedules", new CollectionConverter(mapper));

        xstream.ignoreUnknownElements();

        XStream.setupDefaultSecurity(xstream);
        xstream.allowTypes(new Class[]{Configuration.class});

        return xstream;
    }

}
