package uk.gov.companieshouse.exemptions.util;

import com.mongodb.BasicDBObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.jspecify.annotations.NullMarked;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.exemptions.exception.ExemptionsWriteException;

@WritingConverter
@NullMarked
public class ExemptionsWriteConverter implements Converter<CompanyExemptions, BasicDBObject> {

    private final JsonMapper mapper;

    public ExemptionsWriteConverter(final JsonMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Write convertor.
     * @param source source Document.
     * @return charge BSON object.
     */
    @Override
    public BasicDBObject convert(final CompanyExemptions source) {
        try {
            return BasicDBObject.parse(mapper.writeValueAsString(source));
        } catch (Exception ex) {
            throw new ExemptionsWriteException(ex);
        }
    }
}