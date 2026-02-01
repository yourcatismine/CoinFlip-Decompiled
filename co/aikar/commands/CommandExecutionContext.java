package co.aikar.commands;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandExecutionContext<CEC extends CommandExecutionContext, I extends CommandIssuer> {
   private final RegisteredCommand cmd;
   private final CommandParameter param;
   protected final I issuer;
   private final List<String> args;
   private final int index;
   private final Map<String, Object> passedArgs;
   private final Map<String, String> flags;
   private final CommandManager manager;

   CommandExecutionContext(RegisteredCommand cmd, CommandParameter param, I sender, List<String> args, int index, Map<String, Object> passedArgs) {
      this.cmd = cmd;
      this.manager = cmd.scope.manager;
      this.param = param;
      this.issuer = sender;
      this.args = args;
      this.index = index;
      this.passedArgs = passedArgs;
      this.flags = param.getFlags();
   }

   public String popFirstArg() {
      return !this.args.isEmpty() ? this.args.remove(0) : null;
   }

   public String popLastArg() {
      return !this.args.isEmpty() ? this.args.remove(this.args.size() - 1) : null;
   }

   public String getFirstArg() {
      return !this.args.isEmpty() ? this.args.get(0) : null;
   }

   public String getLastArg() {
      return !this.args.isEmpty() ? this.args.get(this.args.size() - 1) : null;
   }

   public boolean isLastArg() {
      return this.cmd.parameters.length - 1 == this.index;
   }

   public int getNumParams() {
      return this.cmd.parameters.length;
   }

   public boolean canOverridePlayerContext() {
      return this.cmd.requiredResolvers >= this.args.size();
   }

   public Object getResolvedArg(String arg) {
      return this.passedArgs.get(arg);
   }

   public Object getResolvedArg(Class<?>... classes) {
      for (Class<?> clazz : classes) {
         for (Object passedArg : this.passedArgs.values()) {
            if (clazz.isInstance(passedArg)) {
               return passedArg;
            }
         }
      }

      return null;
   }

   public <T> T getResolvedArg(String key, Class<?>... classes) {
      Object o = this.passedArgs.get(key);

      for (Class<?> clazz : classes) {
         if (clazz.isInstance(o)) {
            return (T)o;
         }
      }

      return null;
   }

   public Set<String> getParameterPermissions() {
      return this.param.getRequiredPermissions();
   }

   public boolean isOptional() {
      return this.param.isOptional();
   }

   public boolean hasFlag(String flag) {
      return this.flags.containsKey(flag);
   }

   public String getFlagValue(String flag, String def) {
      return this.flags.getOrDefault(flag, def);
   }

   public Integer getFlagValue(String flag, Integer def) {
      return ACFUtil.parseInt(this.flags.get(flag), def);
   }

   public Long getFlagValue(String flag, Long def) {
      return ACFUtil.parseLong(this.flags.get(flag), def);
   }

   public Float getFlagValue(String flag, Float def) {
      return ACFUtil.parseFloat(this.flags.get(flag), def);
   }

   public Double getFlagValue(String flag, Double def) {
      return ACFUtil.parseDouble(this.flags.get(flag), def);
   }

   public Integer getIntFlagValue(String flag, Number def) {
      return ACFUtil.parseInt(this.flags.get(flag), def != null ? def.intValue() : null);
   }

   public Long getLongFlagValue(String flag, Number def) {
      return ACFUtil.parseLong(this.flags.get(flag), def != null ? def.longValue() : null);
   }

   public Float getFloatFlagValue(String flag, Number def) {
      return ACFUtil.parseFloat(this.flags.get(flag), def != null ? def.floatValue() : null);
   }

   public Double getDoubleFlagValue(String flag, Number def) {
      return ACFUtil.parseDouble(this.flags.get(flag), def != null ? def.doubleValue() : null);
   }

   public Boolean getBooleanFlagValue(String flag) {
      return this.getBooleanFlagValue(flag, false);
   }

   public Boolean getBooleanFlagValue(String flag, Boolean def) {
      String val = this.flags.get(flag);
      return val == null ? def : ACFUtil.isTruthy(val);
   }

   public Double getFlagValue(String flag, Number def) {
      return ACFUtil.parseDouble(this.flags.get(flag), def != null ? def.doubleValue() : null);
   }

   @Deprecated
   public <T extends Annotation> T getAnnotation(Class<T> cls) {
      return this.param.getParameter().getAnnotation(cls);
   }

   public <T extends Annotation> String getAnnotationValue(Class<T> cls) {
      return this.manager.getAnnotations().getAnnotationValue(this.param.getParameter(), cls);
   }

   public <T extends Annotation> String getAnnotationValue(Class<T> cls, int options) {
      return this.manager.getAnnotations().getAnnotationValue(this.param.getParameter(), cls, options);
   }

   public <T extends Annotation> boolean hasAnnotation(Class<T> cls) {
      return this.manager.getAnnotations().hasAnnotation(this.param.getParameter(), cls);
   }

   public RegisteredCommand getCmd() {
      return this.cmd;
   }

   @UnstableAPI
   CommandParameter getCommandParameter() {
      return this.param;
   }

   @Deprecated
   public Parameter getParam() {
      return this.param.getParameter();
   }

   public I getIssuer() {
      return this.issuer;
   }

   public List<String> getArgs() {
      return this.args;
   }

   public int getIndex() {
      return this.index;
   }

   public Map<String, Object> getPassedArgs() {
      return this.passedArgs;
   }

   public Map<String, String> getFlags() {
      return this.flags;
   }

   public String joinArgs() {
      return ACFUtil.join(this.args, " ");
   }

   public String joinArgs(String sep) {
      return ACFUtil.join(this.args, sep);
   }
}
