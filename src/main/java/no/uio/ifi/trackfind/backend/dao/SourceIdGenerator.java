package no.uio.ifi.trackfind.backend.dao;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Properties;

public class SourceIdGenerator extends SequenceStyleGenerator {

    /**
     * {@inheritDoc}
     */
    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        if ((((Source) object).getId()) == null) {
            return super.generate(session, object);
        } else {
            return ((Source) object).getId();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
        params.put(SequenceStyleGenerator.SEQUENCE_PARAM, "source_ids_sequence");
        params.put(SequenceStyleGenerator.INCREMENT_PARAM, SequenceStyleGenerator.DEFAULT_INCREMENT_SIZE);
        super.configure(type, params, serviceRegistry);
    }

}
