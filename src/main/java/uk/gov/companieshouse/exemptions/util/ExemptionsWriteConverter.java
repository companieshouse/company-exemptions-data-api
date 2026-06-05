package uk.gov.companieshouse.exemptions.util;

import com.mongodb.BasicDBObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;

@WritingConverter
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
    public BasicDBObject convert(final @NonNull CompanyExemptions source) {
        try {
            return BasicDBObject.parse(mapper.writeValueAsString(source));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}