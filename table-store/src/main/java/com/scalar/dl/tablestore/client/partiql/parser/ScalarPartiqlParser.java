package com.scalar.dl.tablestore.client.partiql.parser;

import com.scalar.dl.tablestore.client.error.TableStoreClientError;
import com.scalar.dl.tablestore.client.partiql.statement.ContractStatement;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.concurrent.ThreadSafe;
import org.partiql.parser.PartiQLParser;
import org.partiql.spi.Context;
import org.partiql.spi.errors.PErrorKind;
import org.partiql.spi.errors.PErrorListener;
import org.partiql.spi.errors.PRuntimeException;
import org.partiql.spi.errors.Severity;

@ThreadSafe
public class ScalarPartiqlParser {

  private static final PartiQLParser parser = PartiQLParser.standard();
  private static final PErrorListener errorListener =
      error -> {
        Throwable cause = error.get("CAUSE", Throwable.class);
        if (cause instanceof IllegalArgumentException) {
          throw (IllegalArgumentException) cause;
        } else if (error.severity.code() == Severity.ERROR
            && error.kind.code() == PErrorKind.SYNTAX) {
          if (error.location == null) {
            throw new IllegalArgumentException(
                TableStoreClientError.SYNTAX_ERROR_IN_PARTIQL_PARSER.buildMessage(
                    null, null, null, error.name()));
          } else {
            throw new IllegalArgumentException(
                TableStoreClientError.SYNTAX_ERROR_IN_PARTIQL_PARSER.buildMessage(
                    error.location.line,
                    error.location.offset,
                    error.location.length,
                    error.name()));
          }
        }
        throw new PRuntimeException(error);
      };

  private ScalarPartiqlParser() {}

  public static List<ContractStatement> parse(String sql) {
    final PartiqlParserVisitor visitor = new PartiqlParserVisitor();
    return parser.parse(sql, Context.of(errorListener)).statements.stream()
        .flatMap(statement -> statement.accept(visitor, null).stream())
        .collect(Collectors.toList());
  }
}
