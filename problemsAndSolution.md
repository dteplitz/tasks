fix: Address and resolve key issues in plugin development

- Client initialization: Faced significant challenges in initializing the client for the plugin, requiring extensive trial and error, research, and testing. Ultimately resolved by finding the correct initialization method.
- URL path issue: Needed to prepend `/_plugins/` to the URL to avoid conflicts. This issue was resolved after consulting with peers.
- Optional parameters handling: Optional parameters were not being read correctly. After numerous unsuccessful attempts, I opted to use a request body to handle these fields effectively.
- Asynchronous response handling: Struggled with the channel in the controller for async responses, leading to hanging processes. Extensive logging helped identify a working format, which was then applied across the controller.
- Inexperience with OpenSearch plugins: Realized that the controller could be greatly simplified with more experience, potentially by using interfaces, polymorphism, and a design pattern to standardize how the controller handles requests and delegates to services.

These fixes improve the stability and maintainability of the plugin, resolving critical issues and setting the stage for future enhancements.
