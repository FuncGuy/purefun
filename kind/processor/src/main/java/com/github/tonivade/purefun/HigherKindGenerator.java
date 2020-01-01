/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

public class HigherKindGenerator {

  private static final String WITNESS = "µ";
  private static final String NARROWK = "narrowK";
  private static final String KIND = "com.github.tonivade.purefun.Kind";
  private static final String HIGHER1 = "com.github.tonivade.purefun.Higher1";
  private static final String HIGHER2 = "com.github.tonivade.purefun.Higher2";
  private static final String HIGHER3 = "com.github.tonivade.purefun.Higher3";

  private final JavacElements elements;
  private final TreeMaker maker;

  public HigherKindGenerator(JavacElements elements, TreeMaker maker) {
    this.elements = elements;
    this.maker = maker;
  }

  public Optional<JCTree> imports(JCCompilationUnit unit) {
    JCExpression kind = maker.QualIdent(elements.getTypeElement(KIND));
    JCExpression higher1 = maker.QualIdent(elements.getTypeElement(HIGHER1));
    JCExpression higher2 = maker.QualIdent(elements.getTypeElement(HIGHER2));
    JCExpression higher3 = maker.QualIdent(elements.getTypeElement(HIGHER3));
    List<JCTree> newDefs = unit.defs
      .prepend(maker.Import(kind, false))
      .prepend(maker.Import(higher1, false))
      .prepend(maker.Import(higher2, false))
      .prepend(maker.Import(higher3, false));
    unit.defs = newDefs;
    return Optional.of(unit);
  }

  public Optional<JCTree> generate(JCClassDecl clazz, JCAnnotation annotation) {
    Name kindName = kindName(annotation);
    Name varName = elements.getName("hkt");
    JCTree tree = null;
    if (clazz.typarams.length() == 1) {
      tree = generateHigher1Kind(clazz, kindName, varName);
    } else if (clazz.typarams.length() == 2) {
      tree = generateHigher2Kind(clazz, kindName, varName);
    } else if (clazz.typarams.length() == 3) {
      tree = generateHigher3Kind(clazz, kindName, varName);
    } else {
      tree = clazz;
    }
    return Optional.ofNullable(tree);
  }

  private Name kindName(JCAnnotation annotation) {
    Optional<String> name = annotation.args.stream()
      .map(expr -> (JCAssign) expr)
        .filter(assign -> ((JCIdent) assign.lhs).name.contentEquals("name"))
        .findFirst()
        .map(assign -> ((JCLiteral) assign.rhs))
        .map(literal -> (String) literal.value);

    return elements.getName(name.orElse(WITNESS));
  }

  private JCClassDecl generateHigher1Kind(JCClassDecl clazz, Name kindName, Name varName) {
    JCTypeParameter typeParam = clazz.typarams.head;

    JCClassDecl witness = kindWitness(kindName);
    JCTypeApply higher1 = higher1Kind(higher1(select(clazz.name, kindName), typeParam));
    JCMethodDecl narrowKOf1 = narrowKindOf1(higher1, clazz.name, varName, typeParam);
    fixPos(witness, clazz.pos);
    fixPos(narrowKOf1, clazz.pos + witness.pos);

    return maker.ClassDef(
      clazz.mods,
      clazz.name,
      clazz.typarams,
      clazz.extending,
      clazz.implementing.append(higher1Kind(clazz.name, kindName, typeParam)),
      clazz.defs.append(witness).append(narrowKOf1));
  }

  private JCTree generateHigher2Kind(JCClassDecl clazz, Name kindName, Name varName) {
    JCTypeParameter typeParam1 = clazz.typarams.head;
    JCTypeParameter typeParam2 = clazz.typarams.tail.head;

    JCClassDecl witness = kindWitness(kindName);
    JCTypeApply higher1 = nestedHigher1(clazz.name, kindName, typeParam1, typeParam2);
    JCTypeApply higher2 = higher2Kind(higher2(select(clazz.name, kindName), typeParam1, typeParam2));
    JCMethodDecl narrowKOf1 = narrowKindOf1(higher1, clazz.name, varName, typeParam1, typeParam2);
    JCMethodDecl narrowKOf2 = narrowKindOf2(higher2, clazz.name, varName, typeParam1, typeParam2);
    fixPos(witness, clazz.pos);
    fixPos(narrowKOf1, clazz.pos + witness.pos);
    fixPos(narrowKOf2, clazz.pos + witness.pos + narrowKOf1.pos);

    return maker.ClassDef(
      clazz.mods,
      clazz.name,
      clazz.typarams,
      clazz.extending,
      clazz.implementing.append(higher2Kind(clazz.name, kindName, typeParam1, typeParam2)),
      clazz.defs.append(witness).append(narrowKOf1).append(narrowKOf2));
  }

  private JCTree generateHigher3Kind(JCClassDecl clazz, Name kindName, Name varName) {
    JCTypeParameter typeParam1 = clazz.typarams.head;
    JCTypeParameter typeParam2 = clazz.typarams.tail.head;
    JCTypeParameter typeParam3 = clazz.typarams.tail.tail.head;

    JCClassDecl witness = kindWitness(kindName);
    JCTypeApply higher1 = nestedHigher1(clazz.name, kindName, typeParam1, typeParam2, typeParam3);
    JCTypeApply higher2 = nestedHigher2(clazz.name, kindName, typeParam1, typeParam2, typeParam3);
    JCTypeApply higher3 = higher3Kind(higher3(select(clazz.name, kindName), typeParam1, typeParam2, typeParam3));
    JCMethodDecl narrowKOf1 = narrowKindOf1(higher1, clazz.name, varName, typeParam1, typeParam2, typeParam3);
    JCMethodDecl narrowKOf2 = narrowKindOf2(higher2, clazz.name, varName, typeParam1, typeParam2, typeParam3);
    JCMethodDecl narrowKOf3 = narrowKindOf3(higher3, clazz.name, varName, typeParam1, typeParam2, typeParam3);
    fixPos(witness, clazz.pos);
    fixPos(narrowKOf1, clazz.pos + witness.pos);
    fixPos(narrowKOf2, clazz.pos + witness.pos + narrowKOf1.pos);
    fixPos(narrowKOf3, clazz.pos + witness.pos + narrowKOf1.pos + narrowKOf2.pos);

    return maker.ClassDef(
      clazz.mods,
      clazz.name,
      clazz.typarams,
      clazz.extending,
      clazz.implementing.append(higher3Kind(clazz.name, kindName, typeParam1, typeParam2, typeParam3)),
      clazz.defs.append(witness).append(narrowKOf1).append(narrowKOf2).append(narrowKOf3));
  }

  private JCClassDecl kindWitness(Name name) {
    return maker.ClassDef(
        maker.Modifiers(Flags.PUBLIC | Flags.STATIC | Flags.FINAL),
        name,
        List.nil(),
        null,
        List.of(implementsKind()),
        List.nil());
  }

  private JCIdent implementsKind() {
    return maker.Ident(elements.getName("Kind"));
  }

  private JCMethodDecl narrowKindOf1(JCExpression param,
                                     Name className,
                                     Name varName,
                                     JCTypeParameter... typeParams) {
    return maker.MethodDef(
        maker.Modifiers(Flags.PUBLIC | Flags.STATIC),
        elements.getName(NARROWK),
        returnType(className, typeParams),
        params2Type(typeParams),
        List.of(variable(varName, param)),
        List.nil(),
        block(returns(typeCast(className, varName, typeParams))),
        null);
  }

  private JCMethodDecl narrowKindOf2(JCExpression param,
                                     Name className,
                                     Name varName,
                                     JCTypeParameter... typeParams) {
    return maker.MethodDef(
        maker.Modifiers(Flags.PUBLIC | Flags.STATIC),
        elements.getName(NARROWK),
        returnType(className, typeParams),
        params2Type(typeParams),
        List.of(variable(varName, param)),
        List.nil(),
        block(returns(typeCast(className, varName, typeParams))),
        null);
  }

  private JCMethodDecl narrowKindOf3(JCExpression param,
                                     Name className,
                                     Name varName,
                                     JCTypeParameter... typeParams) {
    return maker.MethodDef(
        maker.Modifiers(Flags.PUBLIC | Flags.STATIC),
        elements.getName(NARROWK),
        returnType(className, typeParams),
        params2Type(typeParams),
        List.of(variable(varName, param)),
        List.nil(),
        block(returns(typeCast(className, varName, typeParams))),
        null);
  }

  private JCBlock block(JCReturn returnValue) {
    return maker.Block(0, List.of(returnValue));
  }

  private JCReturn returns(JCExpression expression) {
    return maker.Return(expression);
  }

  private JCTypeCast typeCast(Name className, Name varName, JCTypeParameter... typeParams) {
    return maker.TypeCast(
        returnType(className, typeParams),
        maker.Ident(varName));
  }

  private JCVariableDecl variable(Name varName, JCExpression typeDef) {
    return maker.VarDef(
        maker.Modifiers(Flags.ReceiverParamFlags),
        varName,
        typeDef,
        null);
  }

  private JCTypeApply higher1Kind(Name className,
                                  Name kindName,
                                  JCTypeParameter typeParam) {
    return higher1Kind(higher1(select(className, kindName), typeParam));
  }

  private JCTypeApply higher2Kind(Name className,
                                  Name kindName,
                                  JCTypeParameter typeParam1,
                                  JCTypeParameter typeParam2) {
    return higher2Kind(higher2(select(className, kindName), typeParam1, typeParam2));
  }

  private JCTypeApply higher3Kind(Name className,
                                  Name kindName,
                                  JCTypeParameter typeParam1,
                                  JCTypeParameter typeParam2,
                                  JCTypeParameter typeParam3) {
    return higher3Kind(higher3(select(className, kindName), typeParam1, typeParam2, typeParam3));
  }

  private JCTypeApply higher1Kind(List<JCExpression> type) {
    return maker.TypeApply(maker.Ident(elements.getName("Higher1")), type);
  }

  private JCTypeApply higher2Kind(List<JCExpression> type) {
    return maker.TypeApply(maker.Ident(elements.getName("Higher2")), type);
  }

  private JCTypeApply higher3Kind(List<JCExpression> type) {
    return maker.TypeApply(maker.Ident(elements.getName("Higher3")), type);
  }

  private JCTypeApply nestedHigher1(Name className,
                                    Name kindName,
                                    JCTypeParameter typeParam1,
                                    JCTypeParameter typeParam2)
  {
    return higher1Kind(higher1(higher1Kind(higher1(select(className, kindName), typeParam1)), typeParam2));
  }

  private JCTypeApply nestedHigher1(Name className,
                                    Name kindName,
                                    JCTypeParameter typeParam1,
                                    JCTypeParameter typeParam2,
                                    JCTypeParameter typeParam3) {
    return higher1Kind(higher1(nestedHigher1(className, kindName, typeParam1, typeParam2), typeParam3));
  }

  private JCTypeApply nestedHigher2(Name className,
                                    Name kindName,
                                    JCTypeParameter typeParam1,
                                    JCTypeParameter typeParam2,
                                    JCTypeParameter typeParam3) {
    return higher2Kind(higher2(higher1Kind(higher1(select(className, kindName), typeParam1)), typeParam2, typeParam3));
  }

  private List<JCExpression> higher1(JCExpression nested, JCTypeParameter typeParam) {
    return List.of(nested, maker.Ident(typeParam.name));
  }

  private List<JCExpression> higher2(JCExpression nested,
                                     JCTypeParameter typeParam1,
                                     JCTypeParameter typeParam2) {
    return List.of(nested, maker.Ident(typeParam1.name), maker.Ident(typeParam2.name));
  }

  private List<JCExpression> higher3(JCExpression nested,
                                     JCTypeParameter typeParam1,
                                     JCTypeParameter typeParam2,
                                     JCTypeParameter typeParam3) {
    return List.of(nested, maker.Ident(typeParam1.name), maker.Ident(typeParam2.name), maker.Ident(typeParam3.name));
  }

  private JCTypeApply returnType(Name className, JCTypeParameter... typeParams) {
    return maker.TypeApply(
        maker.Ident(className),
        params2Ident(typeParams));
  }

  private JCFieldAccess select(Name className, Name kindName) {
    return maker.Select(maker.Ident(className), kindName);
  }

  private List<JCExpression> params2Ident(JCTypeParameter... typeParams) {
    return mapParams(this::ident, typeParams);
  }

  private List<JCTypeParameter> params2Type(JCTypeParameter... typeParams) {
    return mapParams(this::typeParam, typeParams);
  }

  private <T extends JCTree> List<T> mapParams(Function<JCTypeParameter, T> mapper, JCTypeParameter... typeParams) {
    return Stream.of(typeParams).map(mapper)
        .reduce(List.nil(), List::append, List::appendList);
  }

  private JCIdent ident(JCTypeParameter typeParam) {
    return maker.Ident(typeParam.name);
  }

  private JCTypeParameter typeParam(JCTypeParameter typeParam) {
    return maker.TypeParameter(typeParam.name, typeParam.bounds, typeParam.annotations);
  }

  private void fixPos(JCTree newTree, int basePos) {
    newTree.accept(new TreeScanner() {
      @Override
      public void scan(JCTree tree) {
        if (tree != null) {
          tree.pos += basePos;
          super.scan(tree);
        }
      }
    });
  }
}
