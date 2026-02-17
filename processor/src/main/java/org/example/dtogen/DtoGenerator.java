package org.example.dtogen;

import org.example.annotation.GenerateDto;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

@SupportedAnnotationTypes("org.example.annotation.GenerateDto")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class DtoGenerator extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(GenerateDto.class)) {
            if (e.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "unlucky, not a class");
                continue;
            }

            TypeElement typeElement = (TypeElement) e;
            GenerateDto anno = typeElement.getAnnotation(GenerateDto.class);

            String dtoPkg = processingEnv.getElementUtils().getPackageOf(typeElement).toString();
            String entityName = typeElement.getSimpleName().toString();

            String dtoName = (anno.name() == null || anno.name().isBlank()) ? entityName + "Dto" : anno.name().trim();

            try {
                writeDto(dtoPkg, dtoName, typeElement);
            } catch(Exception exp){
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write DTO: " + exp.getMessage());
            }
        }
        return true;
    }

    private void writeDto(String dtoPkg, String entityName, TypeElement typeElement) throws IOException {
        String fqcn = dtoPkg + "." + entityName;
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "DtoGenerator running for: " + typeElement.getQualifiedName());
        JavaFileObject file = processingEnv.getFiler().createSourceFile(fqcn, typeElement);
        try (Writer w = file.openWriter()) {
            w.write ("package " + dtoPkg + ";\n\n");
            w.write ("// Generated Dto.\n");
            w.write ("public class " + entityName + "{\n\n");

            var fields = ElementFilter.fieldsIn(typeElement.getEnclosedElements());

            for (VariableElement f : fields) {
                // ignore static
                if (f.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }
                // ignore transient
                if (f.getModifiers().contains(Modifier.TRANSIENT)) {
                    continue;
                }

                String type = f.asType().toString();
                String name = f.getSimpleName().toString();

                w.write("protected " + type + " " + name + ";\n");
            }

            for (VariableElement f : fields) {
                // ignore static
                if (f.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }
                // ignore transient
                if (f.getModifiers().contains(Modifier.TRANSIENT)) {
                    continue;
                }

                String type = f.asType().toString();
                String name = f.getSimpleName().toString();
                String cap = capitalize(name);

                String getterPrefix = isBoolean(type) ? "is" : "get";

                w.write("""
  public %s %s%s() {
    return this.%s;
  }

""".formatted(type, getterPrefix, cap, name));

// setter
                w.write("""
  public void set%s(%s %s) {
    this.%s = %s;
  }

""".formatted(cap, type, name, name, name));
            }
            w.write("}");
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.length() == 1) return s.toUpperCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private boolean isBoolean(String typeName) {
        return "boolean".equals(typeName) || "java.lang.Boolean".equals(typeName);
    }
}
