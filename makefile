#.ONESHELL:
SHELL := /bin/bash

DIR := ./out/production/SocketProxy
Target := $(DIR)/MainKt.class
Proxy := makefile_proxy
KC := kc
JK := jk
#CP := -cp bin -cp '*/*/ref/*.jar'


.PHONY: all
all: $(Target)

$(Target): $(shell find src -name '*kt') | $(DIR)
	$(KC) -d $(DIR) $^

$(DIR):
	mkdir -p $@

.PHONY: run
run:
	$(JK) -cp $(DIR) MainKt

.PHONY: clean
clean:
	rm -rf $(DIR)/* $(Proxy)
