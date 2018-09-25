package no.uio.ifi.trackfind.backend.services;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.SelectUtils;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.data.providers.ihec.IHECDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class TrackFindServiceTest {

    private static final String TEST_DATA_PROVIDER = "TEST";

    @InjectMocks
    private TrackFindService trackFindService;

    @Spy
    private Collection<DataProvider> dataProviders = new HashSet<>();

    @Mock
    private IHECDataProvider dataProvider;

    @Test
    public void getDataProvidersTest() {
        Collection<DataProvider> dataProviders = trackFindService.getDataProviders();
        assertThat(dataProviders).isNotEmpty().hasSize(1);
        DataProvider dataProvider = dataProviders.iterator().next();
        assertThat(dataProvider.getName()).isEqualTo(TEST_DATA_PROVIDER);
    }

    @Test
    public void getDataProviderTest() {
        DataProvider dataProvider = trackFindService.getDataProvider(TEST_DATA_PROVIDER);
        assertThat(dataProvider.getName()).isEqualTo(TEST_DATA_PROVIDER);
    }

    @Test
    public void sql() throws JSQLParserException {
        Select select = SelectUtils.buildSelectFromTableAndExpressions(new Table("datasets"),
                "'analysis_attributes'->'alignment_software' = BISMARK",
                "analysis_attributes->alignment_software IN (BOWTIE, BWA)");
        System.out.println("select = " + select);
    }

    @Before
    public void setUp() {
        when(dataProvider.getName()).thenReturn(TEST_DATA_PROVIDER);
        dataProviders.add(dataProvider);
    }

}
