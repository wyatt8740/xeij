#
# VERSION
#

XEIJ_VERSION = 0160817


#
# ENVIRONMENT
#

JAVA_VERSION = 1.8.0_102

CYG_JDK = /cygdrive/c/Program" "Files/Java/jdk$(JAVA_VERSION)
WIN_JDK = C:/Program" "Files/Java/jdk$(JAVA_VERSION)
WIN_JRE = C:/Program" "Files/Java/jre$(JAVA_VERSION)

JAVA = $(CYG_JDK)/bin/java
JAR = $(CYG_JDK)/bin/jar
JAVAC = $(CYG_JDK)/bin/javac

MKDIR = mkdir -p
RM = rm -f
ZIP = zip


#
# EMULATOR
#

PACKAGE_NAME = xeij
MAIN_CLASS_NAME = XEiJ
CLASS_DIR = class

RESOURCES = IPLROM.DAT IPLROMXV.DAT IPLROMCO.DAT IPLROM30.DAT CGROM_XEiJ.DAT HUMAN.SYS COMMAND.X \
		license_XEiJ.txt license_FSHARP.txt license_MAME.txt
PACKAGE_RESOURCES = $(RESOURCES:%=$(PACKAGE_NAME)/%)

all: XEiJ.jar

clean:
	-$(RM) $(CLASS_DIR)/$(PACKAGE_NAME)/*.class

XEiJ.jar: $(CLASS_DIR)/$(PACKAGE_NAME)/$(MAIN_CLASS_NAME).class $(PACKAGE_RESOURCES)
	-$(RM) $@
	$(JAR) cfe $@ $(PACKAGE_NAME).$(MAIN_CLASS_NAME) -C $(CLASS_DIR) . -C . $(PACKAGE_RESOURCES)

$(CLASS_DIR)/$(PACKAGE_NAME)/$(MAIN_CLASS_NAME).class: $(PACKAGE_NAME)/*.java
	-$(MKDIR) $(CLASS_DIR)/$(PACKAGE_NAME)
	-$(RM) $(CLASS_DIR)/$(PACKAGE_NAME)/*.class
	$(JAVAC) -encoding UTF-8 -d $(CLASS_DIR) $^ -extdirs "" \
		-cp $(WIN_JRE)/lib/plugin.jar";"$(WIN_JRE)/lib/javaws.jar";"$(WIN_JRE)/lib/deploy.jar \
		-Xlint:all -Xlint:-serial -Xlint:-fallthrough -Xdiags:verbose


#
# TEST
#

ifeq ($(BOOT),)
BOOT = xeij
endif

ifneq ($(CLOCK),)
PARAM += -clock $(CLOCK)
endif
ifneq ($(MODEL),)
PARAM += -model $(MODEL)
endif
ifneq ($(ROM),)
PARAM += -rom $(ROM)
endif
ifneq ($(SOUND),)
PARAM += -sound $(SOUND)
endif

test: XEiJ.jar
	$(JAVA) -jar $< -boot $(BOOT) $(PARAM)

human: XEiJ.jar HUMAN302.jar SXWIN315.jar
	$(JAVA) -jar $< -boot=HUMAN302.jar/xeij/HUMAN302.XDF -config=default -fd1=SXWIN315.jar/xeij/SXWIN315.XDF $(PARAM)

otoko: XEiJ.jar OTOKO100.jar
	$(JAVA) -jar $< -boot=OTOKO100.jar/xeij/OTOKO100.2HD -config=default $(PARAM)

jong: XEiJ.jar jong.jar
	$(JAVA) -jar $< -boot=jong.jar/xeij/jong.xdf -config=default $(PARAM)

greversi: XEiJ.jar greversi.jar
	$(JAVA) -jar $< -boot=greversi.jar/xeij/greversi.hdf -config=default -model=EXPERT $(PARAM)

disk060: XEiJ.jar DISK060.jar
	$(JAVA) -jar $< -boot=DISK060.jar/xeij/DISK060I.2HD -config=default -model=060turbo $(PARAM)


