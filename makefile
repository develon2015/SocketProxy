#.ONESHELL:
SHELL := /bin/bash

DIR := ./out/production/SocketProxy
Tajget := $(DIR)/MainKt.class
Jar := ./socketproxy.jar
Proxy := makefile_proxy
KC := kc
JK := jk
#CP := -cp bin -cp '*/*/ref/*.jar'


.PHONY: all
all: $(Target)
jar: $(Jar)

$(Target): $(shell find src -name '*kt') | $(DIR)
	$(KC) -d $(DIR) $^

$(Jar): $(shell find src -name '*kt')
	kotlinc -d $@ $^ -include-runtime

$(DIR):
	mkdir -p $@

.PHONY: run
run:
	$(JK) -cp $(DIR) MainKt

.PHONY: clean
clean:
	rm -rf $(DIR)/* $(Proxy)
