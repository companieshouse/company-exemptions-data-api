package uk.gov.companieshouse.exemptions.util;

import org.bson.Document;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.exemptions.exception.ExemptionsReadException;

@ReadingConverter
@NullMarked
public class ExemptionsReadConverter implements Converter<Document, CompanyExemptions> {

    private final JsonMapper mapper;

    public ExemptionsReadConverter(JsonMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Write convertor.
     * @param source source Document.
     * @return charge BSON object.
     */
    @Override
    public CompanyExemptions convert(final Document source) {
        try {
            return mapper.readValue(source.toJson(), CompanyExemptions.class);
        } catch (Exception ex) {
            throw new ExemptionsReadException(ex);
        }
    }
}