# Makefile for building a single directory of Java source files. It requires
# a DIR variable to be set.

BUILD_DIR := out
DIR := src

PACKAGE := lox
SOURCES := $(wildcard src/com/craftinginterpreters/lox/*.java)
CLASSES = $(patsubst src/%.java, $(BUILD_DIR)/classes/%.class, $(SOURCES))

TOOL_SOURCES := $(wildcard src/com/craftinginterpreters/tool/*.java)
TOOL_CLASSES = $(patsubst src/%.java, $(BUILD_DIR)/gen/classes/%.class, $(TOOL_SOURCES))

JAVA_OPTIONS := -Werror

default: $(CLASSES)
	@: # Don't show "Nothing to be done" output.
tools: $(TOOL_CLASSES)
	@: # Don't show "Nothing to be done" output.
gen_ast: tools
	@ java -cp $(BUILD_DIR)/gen/classes com.craftinginterpreters.tool.GenerateAst \
 			src/com/craftinginterpreters/lox

# Compile a single .java file to .class.
$(BUILD_DIR)/classes/%.class: src/%.java
	@ mkdir -p $(BUILD_DIR)/classes
	@ javac -cp $(DIR) -d $(BUILD_DIR)/classes $(JAVA_OPTIONS) -implicit:none $<
	@ printf "%8s %-60s %s\n" javac $< "$(JAVA_OPTIONS)"

$(BUILD_DIR)/gen/classes/%.class: src/%.java
	@ mkdir -p $(BUILD_DIR)/gen/classes
	@ javac -cp $(DIR) -d $(BUILD_DIR)/gen/classes $(JAVA_OPTIONS) -implicit:none $<
	@ printf "%8s %-60s %s\n" javac $< "$(JAVA_OPTIONS)"

clean:
	@ rm -rf $(BUILD_DIR)
print:
	@ echo "Java Source files: $(TOOL_CLASSES)"
jar: default
	@ echo "Creating jar..."
	@ echo Main-Class: com.craftinginterpreters.lox.Lox > $(BUILD_DIR)/manifest.txt
	@ jar --create --file $(BUILD_DIR)/lox.jar --main-class com.craftinginterpreters.lox.Lox -C $(BUILD_DIR)/classes .
.PHONY: default