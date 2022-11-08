package uk.gov.companieshouse.exemptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;

@ReadingConverter
public class ExemptionsReadConverter implements Converter<Document, CompanyExemptions> {

    private final ObjectMapper objectMapper;

    public ExemptionsReadConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Write convertor.
     * @param source source Document.
     * @return charge BSON object.
     */
    @Override
    public CompanyExemptions convert(Document source) {
        try {
            return objectMapper.readValue(source.toJson(), CompanyExemptions.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
