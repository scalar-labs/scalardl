package com.scalar.dl.tablestore.client.partiql.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.tablestore.client.error.TableStoreClientError;
import com.scalar.dl.tablestore.client.partiql.DataType;
import com.scalar.dl.tablestore.client.partiql.statement.ContractStatement;
import com.scalar.dl.tablestore.client.partiql.statement.CreateTableStatement;
import com.scalar.dl.tablestore.client.partiql.statement.InsertStatement;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Literal;
import org.partiql.ast.ddl.AttributeConstraint;
import org.partiql.ast.ddl.AttributeConstraint.Unique;
import org.partiql.ast.ddl.ColumnDefinition;
import org.partiql.ast.ddl.CreateTable;
import org.partiql.ast.dml.Insert;
import org.partiql.ast.dml.InsertSource;
import org.partiql.ast.expr.Expr;
import org.partiql.ast.expr.ExprArray;
import org.partiql.ast.expr.ExprLit;
import org.partiql.ast.expr.ExprRowValue;
import org.partiql.ast.expr.ExprStruct;
import org.partiql.ast.expr.ExprStruct.Field;
import org.partiql.ast.expr.ExprValues;
import org.partiql.ast.expr.ExprVarRef;
import org.partiql.ast.sql.SqlBlock;
import org.partiql.ast.sql.SqlDialect;
import org.partiql.ast.sql.SqlLayout;

public class PartiQLParserVisitor extends AstVisitor<List<ContractStatement>, Void> {
  private final JacksonSerDe jacksonSerDe = new JacksonSerDe(new ObjectMapper());

  @Override
  public List<ContractStatement> visitCreateTable(CreateTable astNode, Void context) {
    String table = astNode.getName().getIdentifier().getText();
    String primaryKey = null;
    DataType primaryKeyType = null;
    ImmutableMap.Builder<String, DataType> indexes = ImmutableMap.builder();

    for (ColumnDefinition columnDefinition : astNode.getColumns()) {
      DataType dataType = extractDataType(columnDefinition.getDataType());

      List<AttributeConstraint> constraints = columnDefinition.getConstraints();
      if (constraints.isEmpty()) {
        indexes.put(columnDefinition.getName().getText(), dataType);
        continue;
      }

      if (constraints.size() == 1 && constraints.get(0) instanceof Unique) {
        if (primaryKey != null) {
          throw new IllegalArgumentException(
              TableStoreClientError.SYNTAX_ERROR_INVALID_PRIMARY_KEY_SPECIFICATION.buildMessage());
        }
        primaryKey = columnDefinition.getName().getText();
        primaryKeyType = dataType;
      } else {
        throw new IllegalArgumentException(
            TableStoreClientError.SYNTAX_ERROR_INVALID_COLUMN_CONSTRAINTS.buildMessage());
      }
    }

    if (primaryKey == null) {
      throw new IllegalArgumentException(
          TableStoreClientError.SYNTAX_ERROR_INVALID_PRIMARY_KEY_SPECIFICATION.buildMessage());
    }

    return ImmutableList.of(
        CreateTableStatement.create(table, primaryKey, primaryKeyType, indexes.build()));
  }

  @Override
  public List<ContractStatement> visitInsert(Insert astNode, Void context) {
    InsertSource source = astNode.getSource();

    if (astNode.getAsAlias() != null || astNode.getOnConflict() != null) {
      throw new IllegalArgumentException(
          TableStoreClientError.SYNTAX_ERROR_INVALID_INSERT_STATEMENT.buildMessage());
    }

    if (source instanceof InsertSource.FromExpr) {
      String table = astNode.getTableName().getIdentifier().getText();
      InsertSource.FromExpr insertExpr = (InsertSource.FromExpr) source;

      if (insertExpr.getColumns() != null) {
        throw new IllegalArgumentException(
            TableStoreClientError.SYNTAX_ERROR_INVALID_INSERT_STATEMENT.buildMessage());
      }

      if (insertExpr.getExpr() instanceof ExprValues) {
        List<Expr> rowValues = ((ExprValues) insertExpr.getExpr()).getRows();
        if (rowValues.size() != 1
            || !(rowValues.get(0) instanceof ExprRowValue)
            || !(((ExprRowValue) rowValues.get(0)).getValues().get(0) instanceof ExprStruct)) {
          throw new IllegalArgumentException(
              TableStoreClientError.SYNTAX_ERROR_INVALID_INSERT_STATEMENT.buildMessage());
        }

        ExprStruct struct = (ExprStruct) ((ExprRowValue) rowValues.get(0)).getValues().get(0);
        return ImmutableList.of(
            InsertStatement.create(table, convertExprStructToObjectNode(struct)));
      }
    }

    throw new IllegalArgumentException(
        TableStoreClientError.SYNTAX_ERROR_INVALID_INSERT_STATEMENT.buildMessage());
  }

  @Override
  public List<ContractStatement> defaultVisit(AstNode astNode, Void context) {
    throw new IllegalArgumentException(
        TableStoreClientError.SYNTAX_ERROR_INVALID_STATEMENT.buildMessage());
  }

  @Override
  public List<ContractStatement> defaultReturn(AstNode astNode, Void context) {
    throw new IllegalArgumentException(
        TableStoreClientError.SYNTAX_ERROR_INVALID_STATEMENT.buildMessage());
  }

  private DataType extractDataType(org.partiql.ast.DataType dataType) {
    switch (dataType.code()) {
      case org.partiql.ast.DataType.BOOL:
      case org.partiql.ast.DataType.BOOLEAN:
        return DataType.BOOLEAN;
      case org.partiql.ast.DataType.STRING:
        return DataType.STRING;
      case org.partiql.ast.DataType.INT:
      case org.partiql.ast.DataType.INTEGER:
      case org.partiql.ast.DataType.BIGINT:
      case org.partiql.ast.DataType.FLOAT:
      case org.partiql.ast.DataType.DOUBLE_PRECISION:
        return DataType.NUMBER;
      default:
        throw new IllegalArgumentException(
            TableStoreClientError.SYNTAX_ERROR_INVALID_DATA_TYPE.buildMessage(dataType.name()));
    }
  }

  private String extractNameFromExpr(Expr expr) {
    if (expr instanceof ExprVarRef) {
      ExprVarRef varRef = (ExprVarRef) expr;
      return varRef.getIdentifier().getIdentifier().getText();
    }

    throw new IllegalArgumentException(
        TableStoreClientError.SYNTAX_ERROR_INVALID_EXPRESSION.buildMessage(toSql(expr)));
  }

  private ValueNode convertExprLitToValueNode(ExprLit exprLit) {
    Literal literal = exprLit.getLit();
    switch (literal.code()) {
      case Literal.NULL:
        return NullNode.getInstance();
      case Literal.BOOL:
        return BooleanNode.valueOf(literal.booleanValue());
      case Literal.INT_NUM:
        return BigIntegerNode.valueOf(new BigInteger(literal.numberValue()));
      case Literal.APPROX_NUM:
      case Literal.EXACT_NUM:
        return DecimalNode.valueOf(new BigDecimal(literal.numberValue()));
      case Literal.STRING:
        return TextNode.valueOf(literal.stringValue());
      default:
        throw new IllegalArgumentException(
            TableStoreClientError.SYNTAX_ERROR_INVALID_LITERAL.buildMessage(toSql(literal)));
    }
  }

  private ArrayNode convertExprArrayToArrayNode(ExprArray exprArray) {
    ArrayNode array = jacksonSerDe.getObjectMapper().createArrayNode();
    for (Expr expr : exprArray.getValues()) {
      if (expr instanceof ExprLit) {
        array.add(convertExprLitToValueNode((ExprLit) expr));
      } else if (expr instanceof ExprArray) {
        array.add(convertExprArrayToArrayNode((ExprArray) expr));
      } else if (expr instanceof ExprStruct) {
        array.add(convertExprStructToObjectNode((ExprStruct) expr));
      } else {
        throw new IllegalArgumentException(
            TableStoreClientError.SYNTAX_ERROR_INVALID_EXPRESSION.buildMessage(toSql(expr)));
      }
    }
    return array;
  }

  private ObjectNode convertExprStructToObjectNode(ExprStruct exprStruct) {
    ObjectNode object = jacksonSerDe.getObjectMapper().createObjectNode();
    for (Field field : exprStruct.getFields()) {
      // We accept both identifier and string for a field name; i.e., both {a: 1} and {"a": 1} are
      // accepted as the same. Note that, since any strings, e.g., {"a-b": 1}, are valid in the
      // parser, the format of the field name is additionally validated on the contract side, and
      // "a-b" will be rejected there.
      String name = extractNameFromExpr(field.getName());
      Expr expr = field.getValue();
      if (expr instanceof ExprLit) {
        object.set(name, convertExprLitToValueNode((ExprLit) expr));
      } else if (expr instanceof ExprArray) {
        object.set(name, convertExprArrayToArrayNode((ExprArray) expr));
      } else if (expr instanceof ExprStruct) {
        object.set(name, convertExprStructToObjectNode((ExprStruct) expr));
      } else {
        throw new IllegalArgumentException(
            TableStoreClientError.SYNTAX_ERROR_INVALID_EXPRESSION.buildMessage(toSql(expr)));
      }
    }
    return object;
  }

  private String toSql(AstNode astNode) {
    SqlBlock sqlBlock = SqlDialect.getSTANDARD().transform(astNode);
    return SqlLayout.getSTANDARD().print(sqlBlock);
  }
}
