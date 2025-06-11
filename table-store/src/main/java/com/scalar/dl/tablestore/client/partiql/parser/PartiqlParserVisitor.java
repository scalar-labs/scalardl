package com.scalar.dl.tablestore.client.partiql.parser;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.scalar.dl.tablestore.client.partiql.statement.SelectStatement;
import com.scalar.dl.tablestore.client.partiql.statement.UpdateStatement;
import com.scalar.dl.tablestore.client.util.JacksonUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.From;
import org.partiql.ast.FromExpr;
import org.partiql.ast.FromJoin;
import org.partiql.ast.FromTableRef;
import org.partiql.ast.FromType;
import org.partiql.ast.JoinType;
import org.partiql.ast.Literal;
import org.partiql.ast.Query;
import org.partiql.ast.QueryBody;
import org.partiql.ast.Select;
import org.partiql.ast.SelectItem;
import org.partiql.ast.SelectList;
import org.partiql.ast.SelectStar;
import org.partiql.ast.ddl.AttributeConstraint;
import org.partiql.ast.ddl.AttributeConstraint.Unique;
import org.partiql.ast.ddl.ColumnDefinition;
import org.partiql.ast.ddl.CreateTable;
import org.partiql.ast.dml.Insert;
import org.partiql.ast.dml.InsertSource;
import org.partiql.ast.dml.SetClause;
import org.partiql.ast.dml.Update;
import org.partiql.ast.dml.UpdateTarget;
import org.partiql.ast.expr.Expr;
import org.partiql.ast.expr.ExprAnd;
import org.partiql.ast.expr.ExprArray;
import org.partiql.ast.expr.ExprLit;
import org.partiql.ast.expr.ExprNullPredicate;
import org.partiql.ast.expr.ExprOperator;
import org.partiql.ast.expr.ExprPath;
import org.partiql.ast.expr.ExprQuerySet;
import org.partiql.ast.expr.ExprRowValue;
import org.partiql.ast.expr.ExprStruct;
import org.partiql.ast.expr.ExprStruct.Field;
import org.partiql.ast.expr.ExprValues;
import org.partiql.ast.expr.ExprVarRef;
import org.partiql.ast.expr.PathStep;
import org.partiql.ast.sql.SqlBlock;
import org.partiql.ast.sql.SqlDialect;
import org.partiql.ast.sql.SqlLayout;

public class PartiqlParserVisitor extends AstVisitor<List<ContractStatement>, Void> {
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

  private List<JsonNode> getConditions(Expr expr) {
    if (expr == null) {
      return ImmutableList.of();
    }

    if (expr instanceof ExprOperator) {
      ExprOperator operator = (ExprOperator) expr;
      if (operator.getRhs() instanceof ExprLit) {
        String column = getColumn(operator.getLhs());
        JsonNode value = convertExprLitToValueNode((ExprLit) operator.getRhs());
        return ImmutableList.of(JacksonUtils.buildCondition(column, operator.getSymbol(), value));
      }
    } else if (expr instanceof ExprNullPredicate) {
      ExprNullPredicate predicate = (ExprNullPredicate) expr;
      String column = getColumn(predicate.getValue());
      return ImmutableList.of(JacksonUtils.buildNullCondition(column, predicate.isNot()));
    } else if (expr instanceof ExprAnd) {
      ExprAnd exprAnd = (ExprAnd) expr;
      return Stream.concat(
              getConditions(exprAnd.getLhs()).stream(), getConditions(exprAnd.getRhs()).stream())
          .collect(Collectors.toList());
    }

    throw new IllegalArgumentException(
        TableStoreClientError.SYNTAX_ERROR_INVALID_CONDITION.buildMessage(toSql(expr)));
  }

  private String getUpdateTarget(UpdateTarget updateTarget) {
    if (updateTarget.getSteps().isEmpty()) {
      return updateTarget.getRoot().getText();
    }

    throw new IllegalArgumentException(
        TableStoreClientError.SYNTAX_ERROR_INVALID_UPDATE_TARGET.buildMessage());
  }

  @Override
  public List<ContractStatement> visitUpdate(Update astNode, Void context) {
    String table = astNode.getTableName().getIdentifier().getText();
    List<JsonNode> predicates = getConditions(astNode.getCondition());

    ObjectNode values = jacksonSerDe.getObjectMapper().createObjectNode();
    for (SetClause setClause : astNode.getSetClauses()) {
      String column = getUpdateTarget(setClause.getTarget());
      JsonNode value = convertExprToJsonNode(setClause.getExpr());
      values.set(column, value);
    }

    return ImmutableList.of(UpdateStatement.create(table, values, predicates));
  }

  private JsonNode getTable(FromExpr table) {
    String tableName = extractNameFromExpr(table.getExpr());

    if (table.getFromType().code() != FromType.SCAN || table.getAtAlias() != null) {
      throw new IllegalArgumentException(
          TableStoreClientError.SYNTAX_ERROR_INVALID_TABLE.buildMessage(tableName));
    }

    if (table.getAsAlias() == null) {
      return JacksonUtils.buildTable(tableName);
    } else {
      return JacksonUtils.buildTable(tableName, table.getAsAlias().getText());
    }
  }

  private String getColumnReference(ExprPath exprPath) {
    if (exprPath.getSteps().size() == 1) {
      String tableReference = extractNameFromExpr(exprPath.getRoot());
      PathStep step = exprPath.getSteps().get(0);
      if (step instanceof PathStep.Field) {
        String column = ((PathStep.Field) step).getField().getText();
        return JacksonUtils.buildColumnReference(tableReference, column);
      }
    }

    throw new IllegalArgumentException(
        TableStoreClientError.SYNTAX_ERROR_INVALID_COLUMN.buildMessage(toSql(exprPath)));
  }

  private String getColumn(Expr expr) {
    if (expr instanceof ExprVarRef) {
      return extractNameFromExpr(expr);
    } else if (expr instanceof ExprPath) {
      return getColumnReference((ExprPath) expr);
    }

    throw new IllegalArgumentException(
        TableStoreClientError.SYNTAX_ERROR_INVALID_COLUMN.buildMessage(toSql(expr)));
  }

  private String getJoinKey(Expr expr) {
    if (expr instanceof ExprPath) {
      ExprPath exprPath = (ExprPath) expr;
      return getColumnReference(exprPath);
    }

    throw new IllegalArgumentException(
        TableStoreClientError.SYNTAX_ERROR_INVALID_JOIN_CONDITION.buildMessage(toSql(expr)));
  }

  private List<String> getJoinKeys(Expr expr) {
    if (expr instanceof ExprOperator) {
      ExprOperator operator = (ExprOperator) expr;
      if (operator.getSymbol().equals("=")) {
        String leftKey = getJoinKey(operator.getLhs());
        String rightKey = getJoinKey(operator.getRhs());
        return ImmutableList.of(leftKey, rightKey);
      }
    }

    throw new IllegalArgumentException(
        TableStoreClientError.SYNTAX_ERROR_INVALID_JOIN_CONDITION.buildMessage(toSql(expr)));
  }

  private JsonNode getJoin(FromJoin fromJoin, FromExpr rightTable) {
    List<String> joinKeys = getJoinKeys(fromJoin.getCondition());
    return JacksonUtils.buildJoin(getTable(rightTable), joinKeys.get(0), joinKeys.get(1));
  }

  private List<JsonNode> getJoins(FromJoin fromJoin) {
    if (fromJoin.getJoinType() == null || fromJoin.getJoinType().equals(JoinType.INNER())) {
      FromTableRef lhs = fromJoin.getLhs();
      FromTableRef rhs = fromJoin.getRhs();
      if (lhs instanceof FromExpr && rhs instanceof FromExpr) {
        JsonNode leftMostTable = getTable((FromExpr) lhs);
        JsonNode join = getJoin(fromJoin, (FromExpr) rhs);
        return ImmutableList.of(leftMostTable, join);
      } else if (lhs instanceof FromJoin && rhs instanceof FromExpr) {
        return Stream.concat(
                getJoins((FromJoin) lhs).stream(),
                ImmutableList.of(getJoin(fromJoin, (FromExpr) rhs)).stream())
            .collect(Collectors.toList());
      }
    }

    throw new IllegalArgumentException(
        TableStoreClientError.SYNTAX_ERROR_INVALID_JOIN_TYPE.buildMessage());
  }

  private List<String> getProjections(Select select) {
    if (select instanceof SelectStar) {
      SelectStar selectStar = (SelectStar) select;
      if (selectStar.getSetq() != null) {
        throw new IllegalArgumentException(
            TableStoreClientError.SYNTAX_ERROR_SET_QUANTIFIER_NOT_SUPPORTED.buildMessage());
      }

      return ImmutableList.of();
    } else if (select instanceof SelectList) {
      SelectList selectList = (SelectList) select;
      if (selectList.getSetq() != null) {
        throw new IllegalArgumentException(
            TableStoreClientError.SYNTAX_ERROR_SET_QUANTIFIER_NOT_SUPPORTED.buildMessage());
      }

      ImmutableList.Builder<String> builder = ImmutableList.builder();
      for (SelectItem item : selectList.getItems()) {
        if (item instanceof SelectItem.Expr && ((SelectItem.Expr) item).getAsAlias() == null) {
          builder.add(getColumn(((SelectItem.Expr) item).getExpr()));
        } else {
          throw new IllegalArgumentException(
              TableStoreClientError.SYNTAX_ERROR_INVALID_PROJECTION.buildMessage(toSql(item)));
        }
      }

      return builder.build();
    }

    throw new IllegalArgumentException(
        TableStoreClientError.SYNTAX_ERROR_INVALID_SELECT_STATEMENT.buildMessage());
  }

  private int getLimit(Expr expr) {
    if (expr == null) {
      return 0;
    } else if (expr instanceof ExprLit) {
      Literal literal = ((ExprLit) expr).getLit();
      if (literal.code() == Literal.INT_NUM) {
        int limit = Integer.parseInt(literal.numberValue());
        if (limit > 0) {
          return limit;
        }
      }
    }

    throw new IllegalArgumentException(
        TableStoreClientError.SYNTAX_ERROR_INVALID_LIMIT_CLAUSE.buildMessage());
  }

  @Override
  public List<ContractStatement> visitExprQuerySet(ExprQuerySet astNode, Void context) {
    if (astNode.getWith() != null || astNode.getOrderBy() != null || astNode.getOffset() != null) {
      throw new IllegalArgumentException(
          TableStoreClientError.SYNTAX_ERROR_INVALID_SELECT_STATEMENT.buildMessage());
    }

    QueryBody body = astNode.getBody();
    if (body instanceof QueryBody.SFW) {
      QueryBody.SFW sfw = (QueryBody.SFW) body;

      if (sfw.getLet() != null
          || sfw.getExclude() != null
          || sfw.getGroupBy() != null
          || sfw.getHaving() != null) {
        throw new IllegalArgumentException(
            TableStoreClientError.SYNTAX_ERROR_INVALID_SELECT_STATEMENT.buildMessage());
      }

      Select select = sfw.getSelect();
      From from = sfw.getFrom();
      if (from.getTableRefs().size() != 1) {
        throw new IllegalArgumentException(
            TableStoreClientError.SYNTAX_ERROR_INVALID_SELECT_STATEMENT.buildMessage());
      }

      int limit = getLimit(astNode.getLimit());
      if (limit > 0) {
        throw new IllegalArgumentException(
            TableStoreClientError.LIMIT_CLAUSE_NOT_SUPPORTED.buildMessage());
      }

      FromTableRef tableRef = from.getTableRefs().get(0);
      List<JsonNode> conditions = getConditions(sfw.getWhere());
      List<String> projections = getProjections(select);
      if (tableRef instanceof FromJoin) {
        List<JsonNode> joins = getJoins((FromJoin) tableRef);
        return ImmutableList.of(
            SelectStatement.create(
                joins.get(0), joins.subList(1, joins.size()), conditions, projections));
      } else {
        JsonNode table = getTable((FromExpr) tableRef);
        return ImmutableList.of(SelectStatement.create(table, conditions, projections));
      }
    }

    throw new IllegalArgumentException(
        TableStoreClientError.SYNTAX_ERROR_INVALID_STATEMENT.buildMessage());
  }

  @Override
  public List<ContractStatement> visitQuery(Query astNode, Void context) {
    // The Query AST node always has the ExprQuerySet node, and visitExprQuerySet() will be called.
    return astNode.getExpr().accept(this, null);
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

  private JsonNode convertExprToJsonNode(Expr expr) {
    if (expr instanceof ExprLit) {
      return convertExprLitToValueNode((ExprLit) expr);
    } else if (expr instanceof ExprArray) {
      return convertExprArrayToArrayNode((ExprArray) expr);
    } else if (expr instanceof ExprStruct) {
      return convertExprStructToObjectNode((ExprStruct) expr);
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
