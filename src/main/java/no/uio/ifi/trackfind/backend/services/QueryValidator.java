package no.uio.ifi.trackfind.backend.services;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.springframework.stereotype.Service;

@Service
public class QueryValidator {

    public String validate(String query) throws JSQLParserException {
        return CCJSqlParserUtil.parse(query).toString();
    }

}
