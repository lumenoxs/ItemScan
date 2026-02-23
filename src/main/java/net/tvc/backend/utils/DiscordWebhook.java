package net.tvc.backend.utils;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import javax.net.ssl.HttpsURLConnection;
import net.tvc.backend.BackendInstance;

public class DiscordWebhook {
   private final String url;
   private String content;
   private String username;
   private String avatarUrl;
   private boolean tts;
   @SuppressWarnings({ "unchecked", "rawtypes" })
   private List<DiscordWebhook.EmbedObject> embeds = new ArrayList();

   public DiscordWebhook(String url) {
      this.url = url;
   }

   public void setContent(String content) {
      this.content = content;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public void setAvatarUrl(String avatarUrl) {
      this.avatarUrl = avatarUrl;
   }

   public void setTts(boolean tts) {
      this.tts = tts;
   }

   public void addEmbed(DiscordWebhook.EmbedObject embed) {
      this.embeds.add(embed);
   }

   public void execute() {
      if (this.content == null && this.embeds.isEmpty()) {
         throw new IllegalArgumentException("Set content or add at least one EmbedObject");
      } else {
         try {
            DiscordWebhook.JSONObject json = new DiscordWebhook.JSONObject(this);
            json.put("content", this.content);
            json.put("username", this.username);
            json.put("avatar_url", this.avatarUrl);
            json.put("tts", this.tts);
            if (!this.embeds.isEmpty()) {
               @SuppressWarnings({ "unchecked", "rawtypes" })
               List<DiscordWebhook.JSONObject> embedObjects = new ArrayList();
               @SuppressWarnings("rawtypes")
               Iterator var3 = this.embeds.iterator();

               while(true) {
                  if (!var3.hasNext()) {
                     json.put("embeds", embedObjects.toArray());
                     break;
                  }

                  DiscordWebhook.EmbedObject embed = (DiscordWebhook.EmbedObject)var3.next();
                  DiscordWebhook.JSONObject jsonEmbed = new DiscordWebhook.JSONObject(this);
                  jsonEmbed.put("title", embed.getTitle());
                  jsonEmbed.put("description", embed.getDescription());
                  jsonEmbed.put("url", embed.getUrl());
                  if (embed.getColor() != null) {
                     Color color = embed.getColor();
                     int rgb = color.getRed();
                     rgb = (rgb << 8) + color.getGreen();
                     rgb = (rgb << 8) + color.getBlue();
                     jsonEmbed.put("color", rgb);
                  }

                  DiscordWebhook.EmbedObject.Footer footer = embed.getFooter();
                  DiscordWebhook.EmbedObject.Image image = embed.getImage();
                  DiscordWebhook.EmbedObject.Thumbnail thumbnail = embed.getThumbnail();
                  DiscordWebhook.EmbedObject.Author author = embed.getAuthor();
                  List<DiscordWebhook.EmbedObject.Field> fields = embed.getFields();
                  DiscordWebhook.JSONObject jsonAuthor;
                  if (footer != null) {
                     jsonAuthor = new DiscordWebhook.JSONObject(this);
                     jsonAuthor.put("text", footer.getText());
                     jsonAuthor.put("icon_url", footer.getIconUrl());
                     jsonEmbed.put("footer", jsonAuthor);
                  }

                  if (image != null) {
                     jsonAuthor = new DiscordWebhook.JSONObject(this);
                     jsonAuthor.put("url", image.getUrl());
                     jsonEmbed.put("image", jsonAuthor);
                  }

                  if (thumbnail != null) {
                     jsonAuthor = new DiscordWebhook.JSONObject(this);
                     jsonAuthor.put("url", thumbnail.getUrl());
                     jsonEmbed.put("thumbnail", jsonAuthor);
                  }

                  if (author != null) {
                     jsonAuthor = new DiscordWebhook.JSONObject(this);
                     jsonAuthor.put("name", author.getName());
                     jsonAuthor.put("url", author.getUrl());
                     jsonAuthor.put("icon_url", author.getIconUrl());
                     jsonEmbed.put("author", jsonAuthor);
                  }

                  @SuppressWarnings({ "unchecked", "rawtypes" })
                  List<DiscordWebhook.JSONObject> jsonFields = new ArrayList();
                  @SuppressWarnings("rawtypes")
                  Iterator var12 = fields.iterator();

                  while(var12.hasNext()) {
                     DiscordWebhook.EmbedObject.Field field = (DiscordWebhook.EmbedObject.Field)var12.next();
                     DiscordWebhook.JSONObject jsonField = new DiscordWebhook.JSONObject(this);
                     jsonField.put("name", field.getName());
                     jsonField.put("value", field.getValue());
                     jsonField.put("inline", field.isInline());
                     jsonFields.add(jsonField);
                  }

                  jsonEmbed.put("fields", jsonFields.toArray());
                  embedObjects.add(jsonEmbed);
               }
            }

            URL url = URI.create(this.url).toURL();
            HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("User-Agent", "TVC-Backend-Webhook-Client");
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            byte[] payload = json.toString().getBytes(StandardCharsets.UTF_8);

            try {
               OutputStream stream = connection.getOutputStream();

               try {
                  stream.write(payload);
                  stream.flush();
               } catch (Throwable var23) {
                  if (stream != null) {
                     try {
                        stream.close();
                     } catch (Throwable var18) {
                        var23.addSuppressed(var18);
                     }
                  }

                  throw var23;
               }

               if (stream != null) {
                  stream.close();
               }
            } catch (Exception var24) {
               BackendInstance.LOGGER.error("Failed to write Discord webhook output stream", var24);
            }

            int responseCode = connection.getResponseCode();
            InputStream in;
            if (responseCode >= 400) {
               in = connection.getErrorStream();

               try {
                  if (in != null) {
                     BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

                     try {
                        StringBuilder sb = new StringBuilder();

                        while(true) {
                           String line;
                           if ((line = reader.readLine()) == null) {
                              BackendInstance.LOGGER.error("Discord webhook returned HTTP {}: {}", responseCode, sb.toString().trim());
                              break;
                           }

                           sb.append(line).append('\n');
                        }
                     } catch (Throwable var19) {
                        try {
                           reader.close();
                        } catch (Throwable var16) {
                           var19.addSuppressed(var16);
                        }

                        throw var19;
                     }

                     reader.close();
                  } else {
                     BackendInstance.LOGGER.error("Discord webhook returned HTTP {} with empty error stream", responseCode);
                  }
               } catch (Throwable var20) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var15) {
                        var20.addSuppressed(var15);
                     }
                  }

                  throw var20;
               }

               if (in != null) {
                  in.close();
               }
            } else {
               try {
                  in = connection.getInputStream();

                  try {
                     if (in != null) {
                        in.close();
                     }
                  } catch (Throwable var21) {
                     if (in != null) {
                        try {
                           in.close();
                        } catch (Throwable var17) {
                           var21.addSuppressed(var17);
                        }
                     }

                     throw var21;
                  }

                  if (in != null) {
                     in.close();
                  }
               } catch (Exception var22) {
               }
            }

            connection.disconnect();
         } catch (Exception var25) {
            BackendInstance.LOGGER.error("Failed to send Discord webhook", var25);
         }

      }
   }

   private class JSONObject {
      @SuppressWarnings({ "unchecked", "rawtypes" })
      private final HashMap<String, Object> map = new HashMap();

      private JSONObject(final DiscordWebhook param1) {
      }

      void put(String key, Object value) {
         if (value != null) {
            this.map.put(key, value);
         }

      }

      public String toString() {
         StringBuilder builder = new StringBuilder();
         Set<Entry<String, Object>> entrySet = this.map.entrySet();
         builder.append("{");
         int i = 0;
         @SuppressWarnings("rawtypes")
         Iterator var4 = entrySet.iterator();

         while(var4.hasNext()) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Entry<String, Object> entry = (Entry)var4.next();
            Object val = entry.getValue();
            builder.append(this.quote((String)entry.getKey())).append(":");
            if (val instanceof String) {
               builder.append(this.quote(String.valueOf(val)));
            } else if (val instanceof Integer) {
               builder.append(Integer.valueOf(String.valueOf(val)));
            } else if (val instanceof Boolean) {
               builder.append(val);
            } else if (val instanceof DiscordWebhook.JSONObject) {
               builder.append(val.toString());
            } else if (val.getClass().isArray()) {
               builder.append("[");
               int len = Array.getLength(val);

               for(int j = 0; j < len; ++j) {
                  builder.append(Array.get(val, j).toString()).append(j != len - 1 ? "," : "");
               }

               builder.append("]");
            }

            ++i;
            builder.append(i == entrySet.size() ? "}" : ",");
         }

         return builder.toString();
      }

      private String quote(String string) {
         return "\"" + string + "\"";
      }
   }

   public static class EmbedObject {
      private String title;
      private String description;
      private String url;
      private Color color;
      private DiscordWebhook.EmbedObject.Footer footer;
      private DiscordWebhook.EmbedObject.Thumbnail thumbnail;
      private DiscordWebhook.EmbedObject.Image image;
      private DiscordWebhook.EmbedObject.Author author;
      @SuppressWarnings({ "unchecked", "rawtypes" })
      private List<DiscordWebhook.EmbedObject.Field> fields = new ArrayList();

      public String getTitle() {
         return this.title;
      }

      public String getDescription() {
         return this.description;
      }

      public String getUrl() {
         return this.url;
      }

      public Color getColor() {
         return this.color;
      }

      public DiscordWebhook.EmbedObject.Footer getFooter() {
         return this.footer;
      }

      public DiscordWebhook.EmbedObject.Thumbnail getThumbnail() {
         return this.thumbnail;
      }

      public DiscordWebhook.EmbedObject.Image getImage() {
         return this.image;
      }

      public DiscordWebhook.EmbedObject.Author getAuthor() {
         return this.author;
      }

      public List<DiscordWebhook.EmbedObject.Field> getFields() {
         return this.fields;
      }

      public DiscordWebhook.EmbedObject setTitle(String title) {
         this.title = title;
         return this;
      }

      public DiscordWebhook.EmbedObject setDescription(String description) {
         this.description = description;
         return this;
      }

      public DiscordWebhook.EmbedObject setUrl(String url) {
         this.url = url;
         return this;
      }

      public DiscordWebhook.EmbedObject setColor(Color color) {
         this.color = color;
         return this;
      }

      public DiscordWebhook.EmbedObject setFooter(String text, String icon) {
         this.footer = new DiscordWebhook.EmbedObject.Footer(this, text, icon);
         return this;
      }

      public DiscordWebhook.EmbedObject setThumbnail(String url) {
         this.thumbnail = new DiscordWebhook.EmbedObject.Thumbnail(this, url);
         return this;
      }

      public DiscordWebhook.EmbedObject setImage(String url) {
         this.image = new DiscordWebhook.EmbedObject.Image(this, url);
         return this;
      }

      public DiscordWebhook.EmbedObject setAuthor(String name, String url, String icon) {
         this.author = new DiscordWebhook.EmbedObject.Author(this, name, url, icon);
         return this;
      }

      public DiscordWebhook.EmbedObject addField(String name, String value, boolean inline) {
         this.fields.add(new DiscordWebhook.EmbedObject.Field(this, name, value, inline));
         return this;
      }

      private class Footer {
         private String text;
         private String iconUrl;

         private Footer(final DiscordWebhook.EmbedObject param1, String text, String iconUrl) {
            this.text = text;
            this.iconUrl = iconUrl;
         }

         private String getText() {
            return this.text;
         }

         private String getIconUrl() {
            return this.iconUrl;
         }
      }

      private class Thumbnail {
         private String url;

         private Thumbnail(final DiscordWebhook.EmbedObject param1, String url) {
            this.url = url;
         }

         private String getUrl() {
            return this.url;
         }
      }

      private class Image {
         private String url;

         private Image(final DiscordWebhook.EmbedObject param1, String url) {
            this.url = url;
         }

         private String getUrl() {
            return this.url;
         }
      }

      private class Author {
         private String name;
         private String url;
         private String iconUrl;

         private Author(final DiscordWebhook.EmbedObject param1, String name, String url, String iconUrl) {
            this.name = name;
            this.url = url;
            this.iconUrl = iconUrl;
         }

         private String getName() {
            return this.name;
         }

         private String getUrl() {
            return this.url;
         }

         private String getIconUrl() {
            return this.iconUrl;
         }
      }

      private class Field {
         private String name;
         private String value;
         private boolean inline;

         private Field(final DiscordWebhook.EmbedObject param1, String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
         }

         private String getName() {
            return this.name;
         }

         private String getValue() {
            return this.value;
         }

         private boolean isInline() {
            return this.inline;
         }
      }
   }
}
