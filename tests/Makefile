SRCDIR := ../src/ca/dioo/android/

.PHONY: test
test:
	javac -d . $(wildcard *.java) $(filter-out %Activity.java,$(wildcard $(SRCDIR)/*.java)) && java Test

